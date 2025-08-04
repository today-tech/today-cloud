/*
 * Copyright 2021 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package infra.remoting.core;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Closeable;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.DuplexConnection;
import infra.remoting.Payload;
import infra.remoting.ProtocolErrorException;
import infra.remoting.exceptions.InvalidSetupException;
import infra.remoting.exceptions.RejectedSetupException;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.lease.TrackingLeaseSender;
import infra.remoting.plugins.ConnectionDecorator;
import infra.remoting.plugins.InitializingInterceptorRegistry;
import infra.remoting.plugins.InterceptorRegistry;
import infra.remoting.plugins.RateLimitDecorator;
import infra.remoting.resume.SessionManager;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import infra.remoting.transport.Transport;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import static infra.remoting.core.FragmentationUtils.assertMtu;
import static infra.remoting.core.PayloadValidationUtils.assertValidateSetup;
import static infra.remoting.core.ReassemblyUtils.assertInboundPayloadSize;
import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

/**
 * The main class for starting a server.
 *
 * <p>For example:
 *
 * <pre>{@code
 * CloseableChannel closeable =
 *         RemotingServer.create(ChannelAcceptor.with(new Channel() {...}))
 *                 .bind(TcpServerTransport.create("localhost", 7000))
 *                 .block();
 * }</pre>
 */
public final class RemotingServer {

  private static final String SERVER_TAG = "server";

  private ChannelAcceptor acceptor = ChannelAcceptor.with(new Channel() { });

  private final InitializingInterceptorRegistry interceptors = new InitializingInterceptorRegistry();

  private Resume resume;

  private Consumer<LeaseSpec> leaseConfigurer = null;

  private int mtu = 0;

  private int maxInboundPayloadSize = Integer.MAX_VALUE;

  private PayloadDecoder payloadDecoder = PayloadDecoder.DEFAULT;

  private Duration timeout = Duration.ofMinutes(1);

  private RemotingServer() {
  }

  /** Static factory method to create an {@code RemotingServer}. */
  public static RemotingServer create() {
    return new RemotingServer();
  }

  /**
   * Static factory method to create an {@code RemotingServer} instance with the given {@code
   * ChannelAcceptor}. Effectively a shortcut for:
   *
   * <pre class="code">
   * RemotingServer.create().acceptor(...);
   * </pre>
   *
   * @param acceptor the acceptor to handle connections with
   * @return the same instance for method chaining
   * @see #acceptor(ChannelAcceptor)
   */
  public static RemotingServer create(ChannelAcceptor acceptor) {
    return RemotingServer.create().acceptor(acceptor);
  }

  /**
   * Set the acceptor to handle incoming connections and handle requests.
   *
   * <p>An example with access to the {@code SETUP} frame and sending Channel for performing
   * requests back to the client if needed:
   *
   * <pre>{@code
   * RemotingServer.create((setup, sending) -> Mono.just(new Channel() {...}))
   *         .bind(TcpServerTransport.create("localhost", 7000))
   *         .subscribe();
   * }</pre>
   *
   * <p>A shortcut to provide the handling Channel only:
   *
   * <pre>{@code
   * RemotingServer.create(ChannelAcceptor.with(new Channel() {...}))
   *         .bind(TcpServerTransport.create("localhost", 7000))
   *         .subscribe();
   * }</pre>
   *
   * <p>A shortcut to handle request-response interactions only:
   *
   * <pre>{@code
   * RemotingServer.create(ChannelAcceptor.forRequestResponse(payload -> ...))
   *         .bind(TcpServerTransport.create("localhost", 7000))
   *         .subscribe();
   * }</pre>
   *
   * <p>By default, {@code new Channel(){}} is used for handling which rejects requests from the
   * client with {@link UnsupportedOperationException}.
   *
   * @param acceptor the acceptor to handle incoming connections and requests with
   * @return the same instance for method chaining
   */
  public RemotingServer acceptor(ChannelAcceptor acceptor) {
    Objects.requireNonNull(acceptor);
    this.acceptor = acceptor;
    return this;
  }

  /**
   * Configure interception at one of the following levels:
   *
   * <ul>
   *   <li>Transport level
   *   <li>At the level of accepting new connections
   *   <li>Performing requests
   *   <li>Responding to requests
   * </ul>
   *
   * @param configurer a configurer to customize interception with.
   * @return the same instance for method chaining
   * @see RateLimitDecorator
   */
  public RemotingServer interceptors(Consumer<InterceptorRegistry> configurer) {
    configurer.accept(this.interceptors);
    return this;
  }

  /**
   * Enables the Resume capability of the protocol where if the client gets disconnected,
   * the connection is re-acquired and any interrupted streams are transparently resumed. For this
   * to work clients must also support and request to enable this when connecting.
   *
   * <p>Use the {@link Resume} argument to customize the Resume session duration, storage, retry
   * logic, and others.
   *
   * <p>By default this is not enabled.
   *
   * @param resume configuration for the Resume capability
   * @return the same instance for method chaining
   * @see <a
   * href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#resuming-operation">Resuming
   * Operation</a>
   */
  public RemotingServer resume(Resume resume) {
    this.resume = resume;
    return this;
  }

  /**
   * Enables the Lease feature of the protocol where the number of requests that can be
   * performed from either side are rationed via {@code LEASE} frames from the responder side. For
   * this to work clients must also support and request to enable this when connecting.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * RemotingServer.create(ChannelAcceptor.with(new Channel() {...}))
   *         .lease(spec ->
   *            spec.sender(() -> Flux.interval(ofSeconds(1))
   *                                  .map(__ -> Lease.create(ofSeconds(1), 1)))
   *         )
   *         .bind(TcpServerTransport.create("localhost", 7000))
   *         .subscribe();
   * }</pre>
   *
   * <p>By default this is not enabled.
   *
   * @param leaseConfigurer consumer which accepts {@link LeaseSpec} and use it for configuring
   * @return the same instance for method chaining
   * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#lease-semantics">Lease
   * Semantics</a>
   */
  public RemotingServer lease(Consumer<LeaseSpec> leaseConfigurer) {
    this.leaseConfigurer = leaseConfigurer;
    return this;
  }

  /**
   * When this is set, frames reassembler control maximum payload size which can be reassembled.
   *
   * <p>By default this is not set in which case maximum reassembled payloads size is not
   * controlled.
   *
   * @param maxInboundPayloadSize the threshold size for reassembly, must no be less than 64 bytes.
   * Please note, {@code maxInboundPayloadSize} must always be greater or equal to {@link
   * Transport#getMaxFrameLength()}, otherwise inbound frame can exceed the
   * {@code maxInboundPayloadSize}
   * @return the same instance for method chaining
   * @see <a
   * href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#fragmentation-and-reassembly">Fragmentation
   * and Reassembly</a>
   */
  public RemotingServer maxInboundPayloadSize(int maxInboundPayloadSize) {
    this.maxInboundPayloadSize = assertInboundPayloadSize(maxInboundPayloadSize);
    return this;
  }

  /**
   * Specify the max time to wait for the first frame (e.g. {@code SETUP}) on an accepted
   * connection.
   *
   * <p>By default this is set to 1 minute.
   *
   * @param timeout duration
   * @return the same instance for method chaining
   */
  public RemotingServer maxTimeToFirstFrame(Duration timeout) {
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("Setup Handling Timeout should be greater than zero");
    }
    this.timeout = timeout;
    return this;
  }

  /**
   * When this is set, frames larger than the given maximum transmission unit (mtu) size value are
   * fragmented.
   *
   * <p>By default this is not set in which case payloads are sent whole up to the maximum frame
   * size of 16,777,215 bytes.
   *
   * @param mtu the threshold size for fragmentation, must be no less than 64
   * @return the same instance for method chaining
   * @see <a
   * href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#fragmentation-and-reassembly">Fragmentation
   * and Reassembly</a>
   */
  public RemotingServer fragment(int mtu) {
    this.mtu = assertMtu(mtu);
    return this;
  }

  /**
   * Configure the {@code PayloadDecoder} used to create {@link Payload}'s from incoming raw frame
   * buffers. The following decoders are available:
   *
   * <ul>
   *   <li>{@link PayloadDecoder#DEFAULT} -- the data and metadata are independent copies of the
   *       underlying frame {@link ByteBuf}
   *   <li>{@link PayloadDecoder#ZERO_COPY} -- the data and metadata are retained slices of the
   *       underlying {@link ByteBuf}. That's more efficient but requires careful tracking and
   *       {@link Payload#release() release} of the payload when no longer needed.
   * </ul>
   *
   * <p>By default this is set to {@link PayloadDecoder#DEFAULT} in which case data and metadata are
   * copied and do not need to be tracked and released.
   *
   * @param decoder the decoder to use
   * @return the same instance for method chaining
   */
  public RemotingServer payloadDecoder(PayloadDecoder decoder) {
    Objects.requireNonNull(decoder);
    this.payloadDecoder = decoder;
    return this;
  }

  /**
   * Start the server on the given transport.
   *
   * @param transport the transport of choice to connect with
   * @param <T> the type of {@code Closeable} for the given transport
   * @return a {@code Mono} with a {@code Closeable} that can be used to obtain information about
   * the server, stop it, or be notified of when it is stopped.
   */
  public <T extends Closeable> Mono<T> bind(ServerTransport<T> transport) {
    return Mono.defer(new Supplier<Mono<T>>() {
      private final ServerSetup serverSetup = serverSetup(timeout);

      @Override
      public Mono<T> get() {
        int maxFrameLength = transport.getMaxFrameLength();
        assertValidateSetup(maxFrameLength, maxInboundPayloadSize, mtu);
        return transport
                .start(duplexConnection -> acceptor(serverSetup, duplexConnection, maxFrameLength))
//                .publishOn(Schedulers.boundedElastic())
                .doOnNext(c -> c.onClose().doFinally(v -> serverSetup.dispose()).subscribe());
      }
    });
  }

  /**
   * Start the server on the given transport. Effectively is a shortcut for {@code
   * .bind(ServerTransport).block()}
   */
  public <T extends Closeable> T bindNow(ServerTransport<T> transport) {
    return bind(transport).block();
  }

  /**
   * An alternative to {@link #bind(ServerTransport)} that is useful for installing Channel on a
   * server that is started independently.
   */
  public ConnectionAcceptor asConnectionAcceptor() {
    return asConnectionAcceptor(FRAME_LENGTH_MASK);
  }

  /**
   * An alternative to {@link #bind(ServerTransport)} that is useful for installing Channel on a
   * server that is started independently.
   */
  public ConnectionAcceptor asConnectionAcceptor(int maxFrameLength) {
    assertValidateSetup(maxFrameLength, maxInboundPayloadSize, mtu);
    return new ConnectionAcceptor() {
      private final ServerSetup serverSetup = serverSetup(timeout);

      @Override
      public Mono<Void> accept(DuplexConnection connection) {
        return acceptor(serverSetup, connection, maxFrameLength);
      }
    };
  }

  private Mono<Void> acceptor(ServerSetup serverSetup, DuplexConnection sourceConnection, int maxFrameLength) {
    final DuplexConnection interceptedConnection = interceptors.initConnection(ConnectionDecorator.Type.SOURCE, sourceConnection);
    return serverSetup
            .init(LoggingDuplexConnection.wrapIfEnabled(interceptedConnection))
            .flatMap(tuple2 -> {
              final ByteBuf startFrame = tuple2.getT1();
              final DuplexConnection clientServerConnection = tuple2.getT2();

              return accept(serverSetup, startFrame, clientServerConnection, maxFrameLength);
            });
  }

  private Mono<Void> acceptResume(ServerSetup serverSetup, ByteBuf resumeFrame, DuplexConnection clientServerConnection) {
    return serverSetup.acceptChannelResume(resumeFrame, clientServerConnection);
  }

  private Mono<Void> accept(ServerSetup serverSetup, ByteBuf startFrame, DuplexConnection clientServerConnection, int maxFrameLength) {
    return switch (FrameHeaderCodec.frameType(startFrame)) {
      case SETUP -> acceptSetup(serverSetup, startFrame, clientServerConnection, maxFrameLength);
      case RESUME -> acceptResume(serverSetup, startFrame, clientServerConnection);
      default -> {
        serverSetup.sendError(clientServerConnection, new InvalidSetupException("SETUP or RESUME frame must be received before any others"));
        yield clientServerConnection.onClose();
      }
    };
  }

  private Mono<Void> acceptSetup(ServerSetup serverSetup, ByteBuf setupFrame, DuplexConnection clientServerConnection, int maxFrameLength) {
    if (!SetupFrameCodec.isSupportedVersion(setupFrame)) {
      serverSetup.sendError(clientServerConnection, new InvalidSetupException(
              "Unsupported version: " + SetupFrameCodec.humanReadableVersion(setupFrame)));
      return clientServerConnection.onClose();
    }

    boolean leaseEnabled = leaseConfigurer != null;
    if (SetupFrameCodec.honorLease(setupFrame) && !leaseEnabled) {
      serverSetup.sendError(clientServerConnection, new InvalidSetupException("lease is not supported"));
      return clientServerConnection.onClose();
    }

    return serverSetup.acceptChannelSetup(setupFrame, clientServerConnection, (keepAliveHandler, wrappedDuplexConnection) -> {
      final InitializingInterceptorRegistry interceptors = this.interceptors;
      final ConnectionSetupPayload setupPayload = new DefaultConnectionSetupPayload(setupFrame.retain());
      final ClientServerInputMultiplexer multiplexer = new ClientServerInputMultiplexer(wrappedDuplexConnection, interceptors, false);
      final LeaseSpec leases;
      final RequesterLeaseTracker requesterLeaseTracker;
      if (leaseEnabled) {
        leases = new LeaseSpec();
        leaseConfigurer.accept(leases);
        requesterLeaseTracker = new RequesterLeaseTracker(SERVER_TAG, leases.maxPendingRequests);
      }
      else {
        leases = null;
        requesterLeaseTracker = null;
      }

      final Sinks.Empty<Void> requesterOnAllClosedSink = Sinks.unsafe().empty();
      final Sinks.Empty<Void> responderOnAllClosedSink = Sinks.unsafe().empty();

      Channel requesterChannel = new RequesterChannel(multiplexer.asServerConnection(), payloadDecoder, StreamIdProvider.forServer(),
              mtu, maxFrameLength, maxInboundPayloadSize, setupPayload.keepAliveInterval(), setupPayload.keepAliveMaxLifetime(),
              keepAliveHandler, interceptors::initRequesterRequestInterceptor, requesterLeaseTracker, requesterOnAllClosedSink,
              Mono.whenDelayError(responderOnAllClosedSink.asMono(), requesterOnAllClosedSink.asMono()));

      Channel wrappedChannelRequester = interceptors.decorateRequester(requesterChannel);

      return interceptors
              .decorateAcceptor(acceptor)
              .accept(setupPayload, wrappedChannelRequester)
              .onErrorResume(err -> Mono.fromRunnable(() -> serverSetup.sendError(
                              wrappedDuplexConnection, rejectedSetupError(err)))
                      .then(wrappedDuplexConnection.onClose())
                      .then(Mono.error(err)))
              .doOnNext(channelHandler -> {
                Channel wrappedChannelHandler = interceptors.decorateResponder(channelHandler);
                DuplexConnection clientConnection = multiplexer.asClientConnection();

                ResponderLeaseTracker responderLeaseTracker = leaseEnabled
                        ? new ResponderLeaseTracker(SERVER_TAG, clientConnection, leases.sender)
                        : null;

                Channel channelResponder = new ResponderChannel(clientConnection, wrappedChannelHandler, payloadDecoder, responderLeaseTracker,
                        mtu, maxFrameLength, maxInboundPayloadSize,
                        leaseEnabled && leases.sender instanceof TrackingLeaseSender
                                ? channel -> interceptors.initResponderRequestInterceptor(channel, (TrackingLeaseSender) leases.sender)
                                : interceptors::initResponderRequestInterceptor, responderOnAllClosedSink);
              })
              .doFinally(signalType -> setupPayload.release())
              .then();
    });
  }

  private ServerSetup serverSetup(Duration timeout) {
    return resume != null ? createSetup(timeout) : new ServerSetup.DefaultServerSetup(timeout);
  }

  ServerSetup createSetup(Duration timeout) {
    return new ServerSetup.ResumableServerSetup(timeout, new SessionManager(), resume.getSessionDuration(),
            resume.getStreamTimeout(), resume.getStoreFactory(SERVER_TAG), resume.isCleanupStoreOnKeepAlive());
  }

  private ProtocolErrorException rejectedSetupError(Throwable err) {
    String msg = err.getMessage();
    return new RejectedSetupException(msg == null ? "rejected by server acceptor" : msg);
  }

}
