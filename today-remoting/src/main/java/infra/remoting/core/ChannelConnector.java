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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.lang.Nullable;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Closeable;
import infra.remoting.Connection;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.Payload;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.keepalive.KeepAliveHandler;
import infra.remoting.lease.TrackingLeaseSender;
import infra.remoting.plugins.ConnectionDecorator;
import infra.remoting.plugins.InitializingInterceptorRegistry;
import infra.remoting.plugins.InterceptorRegistry;
import infra.remoting.plugins.RateLimitDecorator;
import infra.remoting.resume.ClientChannelSession;
import infra.remoting.resume.ResumableConnection;
import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.Transport;
import infra.remoting.util.ByteBufPayload;
import infra.remoting.util.DefaultPayload;
import infra.remoting.util.EmptyPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import static infra.remoting.core.FragmentationUtils.assertMtu;
import static infra.remoting.core.PayloadValidationUtils.assertValidateSetup;
import static infra.remoting.core.ReassemblyUtils.assertInboundPayloadSize;

/**
 * The main class to use to establish a connection to a server.
 *
 * <p>For using TCP using default settings:
 *
 * <pre>{@code
 * import infra.remoting.transport.netty.client.TcpClientTransport;
 *
 * Mono<Channel> source =
 *         ChannelConnector.connectWith(TcpClientTransport.create("localhost", 7000));
 * RemotingClient client = RemotingClient.from(source);
 * }</pre>
 *
 * <p>To customize connection settings before connecting:
 *
 * <pre>{@code
 * Mono<Channel> source = ChannelConnector.create()
 *      .connect(TcpClientTransport.create("localhost", 7000));
 * RemotingClient client = RemotingClient.from(source);
 * }</pre>
 */
public class ChannelConnector {

  private static final String CLIENT_TAG = "client";

  private static final BiConsumer<Channel, Invalidatable> INVALIDATE_FUNCTION =
          (r, i) -> r.onClose().subscribe(null, __ -> i.invalidate(), i::invalidate);

  private Mono<Payload> setupPayloadMono = Mono.empty();

  @Deprecated
  private String metadataMimeType = "application/binary";

  @Deprecated
  private String dataMimeType = "application/binary";

  private Duration keepAliveInterval = Duration.ofSeconds(20);

  private Duration keepAliveMaxLifeTime = Duration.ofSeconds(90);

  @Nullable
  private ChannelAcceptor acceptor;

  private final InitializingInterceptorRegistry interceptors = new InitializingInterceptorRegistry();

  @Nullable
  private Retry retrySpec;

  @Nullable
  private Resume resume;

  @Nullable
  private Consumer<LeaseSpec> leaseConfigurer;

  private int mtu = 0;

  private int maxInboundPayloadSize = Integer.MAX_VALUE;

  private PayloadDecoder payloadDecoder = PayloadDecoder.DEFAULT;

  private ChannelConnector() {
  }

  /**
   * Provide a {@code Mono} from which to obtain the {@code Payload} for the initial SETUP frame.
   * Data and metadata should be formatted according to the MIME types specified via {@link
   * #dataMimeType(String)} and {@link #metadataMimeType(String)}.
   *
   * @param setupPayloadMono the payload with data and/or metadata for the {@code SETUP} frame.
   * @return the same instance for method chaining
   * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#frame-setup">SETUP
   * Frame</a>
   */
  public ChannelConnector setupPayload(Mono<Payload> setupPayloadMono) {
    this.setupPayloadMono = setupPayloadMono;
    return this;
  }

  /**
   * Variant of {@link #setupPayload(Mono)} that accepts a {@code Payload} instance.
   *
   * <p>Note: if the given payload is {@link ByteBufPayload}, it is copied to a
   * {@link DefaultPayload} and released immediately. This ensures it can re-used to obtain a
   * connection more than once.
   *
   * @param payload the payload with data and/or metadata for the {@code SETUP} frame.
   * @return the same instance for method chaining
   * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#frame-setup">SETUP
   * Frame</a>
   */
  public ChannelConnector setupPayload(Payload payload) {
    if (payload instanceof DefaultPayload) {
      this.setupPayloadMono = Mono.just(payload);
    }
    else {
      this.setupPayloadMono = Mono.just(DefaultPayload.create(Objects.requireNonNull(payload)));
      payload.release();
    }
    return this;
  }

  /**
   * Set the MIME type to use for formatting payload data on the established connection. This is set
   * in the initial {@code SETUP} frame sent to the server.
   *
   * <p>By default this is set to {@code "application/binary"}.
   *
   * @param dataMimeType the MIME type to be used for payload data
   * @return the same instance for method chaining
   * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#frame-setup">SETUP
   * Frame</a>
   */
  public ChannelConnector dataMimeType(String dataMimeType) {
    this.dataMimeType = Objects.requireNonNull(dataMimeType);
    return this;
  }

  /**
   * Set the MIME type to use for formatting payload metadata on the established connection. This is
   * set in the initial {@code SETUP} frame sent to the server.
   *
   * @param metadataMimeType the MIME type to be used for payload metadata
   * @return the same instance for method chaining
   * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#frame-setup">SETUP
   * Frame</a>
   */
  public ChannelConnector metadataMimeType(String metadataMimeType) {
    this.metadataMimeType = Objects.requireNonNull(metadataMimeType);
    return this;
  }

  /**
   * Set the "Time Between {@code KEEPALIVE} Frames" which is how frequently {@code KEEPALIVE}
   * frames should be emitted, and the "Max Lifetime" which is how long to allow between {@code
   * KEEPALIVE} frames from the remote end before concluding that connectivity is lost. Both
   * settings are specified in the initial {@code SETUP} frame sent to the server. The spec mentions
   * the following:
   *
   * <ul>
   *   <li>For server-to-server connections, a reasonable time interval between client {@code
   *       KEEPALIVE} frames is 500ms.
   *   <li>For mobile-to-server connections, the time interval between client {@code KEEPALIVE}
   *       frames is often {@code >} 30,000ms.
   * </ul>
   *
   * <p>By default these are set to 20 seconds and 90 seconds respectively.
   *
   * @param interval how frequently to emit KEEPALIVE frames
   * @param maxLifeTime how long to allow between {@code KEEPALIVE} frames from the remote end
   * before assuming that connectivity is lost; the value should be generous and allow for
   * multiple missed {@code KEEPALIVE} frames.
   * @return the same instance for method chaining
   * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#frame-setup">SETUP
   * Frame</a>
   */
  public ChannelConnector keepAlive(Duration interval, Duration maxLifeTime) {
    if (!interval.negated().isNegative()) {
      throw new IllegalArgumentException("`interval` for keepAlive must be > 0");
    }
    if (!maxLifeTime.negated().isNegative()) {
      throw new IllegalArgumentException("`maxLifeTime` for keepAlive must be > 0");
    }
    this.keepAliveInterval = interval;
    this.keepAliveMaxLifeTime = maxLifeTime;
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
  public ChannelConnector interceptors(Consumer<InterceptorRegistry> configurer) {
    configurer.accept(this.interceptors);
    return this;
  }

  /**
   * Configure a client-side {@link ChannelAcceptor} for responding to requests from the server.
   *
   * <p>A full-form example with access to the {@code SETUP} frame and the "sending" Channel (the
   * same as the one returned from {@link #connect(ClientTransport)}):
   *
   * <pre>{@code
   * Mono<Channel> channelMono =
   *     ChannelConnector.create()
   *             .acceptor((setup, sending) -> Mono.just(new Channel() {...}))
   *             .connect(transport);
   * }</pre>
   *
   * <p>A shortcut example with just the handling Channel:
   *
   * <pre>{@code
   * Mono<Channel> channelMono =
   *     ChannelConnector.create()
   *             .acceptor(ChannelAcceptor.with(new Channel() {...})))
   *             .connect(transport);
   * }</pre>
   *
   * <p>A shortcut example handling only request-response:
   *
   * <pre>{@code
   * Mono<Channel> channelMono =
   *     ChannelConnector.create()
   *             .acceptor(ChannelAcceptor.forRequestResponse(payload -> ...))
   *             .connect(transport);
   * }</pre>
   *
   * <p>By default, {@code new Channel(){}} is used which rejects all requests from the server with
   * {@link UnsupportedOperationException}.
   *
   * @param acceptor the acceptor to use for responding to server requests
   * @return the same instance for method chaining
   */
  public ChannelConnector acceptor(@Nullable ChannelAcceptor acceptor) {
    this.acceptor = acceptor;
    return this;
  }

  /**
   * When this is enabled, the connect methods of this class return a special {@code Mono<Channel>}
   * that maintains a single, shared {@code Channel} for all subscribers:
   *
   * <pre>{@code
   * Mono<Channel> channelMono =
   *   ChannelConnector.create()
   *           .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
   *           .connect(transport);
   *
   *  Channel r1 = channelMono.block();
   *  Channel r2 = channelMono.block();
   *
   *  assert r1 == r2;
   * }</pre>
   *
   * <p>The {@code Channel} remains cached until the connection is lost and after that, new attempts
   * to subscribe or re-subscribe trigger a reconnect and result in a new shared {@code Channel}:
   *
   * <pre>{@code
   * Mono<Channel> channelMono =
   *   ChannelConnector.create()
   *           .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
   *           .connect(transport);
   *
   *  Channel r1 = channelMono.block();
   *  Channel r2 = channelMono.block();
   *
   *  r1.dispose();
   *
   *  Channel r3 = channelMono.block();
   *  Channel r4 = channelMono.block();
   *
   *  assert r1 == r2;
   *  assert r3 == r4;
   *  assert r1 != r3;
   *
   * }</pre>
   *
   * <p>Downstream subscribers for individual requests still need their own retry logic to determine
   * if or when failed requests should be retried which in turn triggers the shared reconnect:
   *
   * <pre>{@code
   * Mono<Channel> rocketMono =
   *   ChannelConnector.create()
   *           .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
   *           .connect(transport);
   *
   *  channelMono.flatMap(channel -> channel.requestResponse(...))
   *           .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(5)))
   *           .subscribe()
   * }</pre>
   *
   * <p><strong>Note:</strong> this feature is mutually exclusive with {@link #resume(Resume)}. If
   * both are enabled, "resume" takes precedence. Consider using "reconnect" when the server does
   * not have "resume" enabled or supported, or when you don't need to incur the overhead of saving
   * in-flight frames to be potentially replayed after a reconnect.
   *
   * <p>By default this is not enabled in which case a new connection is obtained per subscriber.
   *
   * @param retry a retry spec that declares the rules for reconnecting
   * @return the same instance for method chaining
   */
  public ChannelConnector reconnect(Retry retry) {
    this.retrySpec = Objects.requireNonNull(retry);
    return this;
  }

  /**
   * Enables the Resume capability of the protocol where if the client gets disconnected,
   * the connection is re-acquired and any interrupted streams are resumed automatically. For this
   * to work the server must also support and have the Resume capability enabled.
   *
   * <p>See {@link Resume} for settings to customize the Resume capability.
   *
   * <p><strong>Note:</strong> this feature is mutually exclusive with {@link #reconnect(Retry)}. If
   * both are enabled, "resume" takes precedence. Consider using "reconnect" when the server does
   * not have "resume" enabled or supported, or when you don't need to incur the overhead of saving
   * in-flight frames to be potentially replayed after a reconnect.
   *
   * <p>By default this is not enabled.
   *
   * @param resume configuration for the Resume capability
   * @return the same instance for method chaining
   * @see <a
   * href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#resuming-operation">Resuming
   * Operation</a>
   */
  public ChannelConnector resume(Resume resume) {
    this.resume = resume;
    return this;
  }

  /**
   * Enables the Lease feature of the protocol where the number of requests that can be
   * performed from either side are rationed via {@code LEASE} frames from the responder side.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * Mono<Channel> rocketMono =
   *         ChannelConnector.create()
   *                         .lease()
   *                         .connect(transport);
   * }</pre>
   *
   * <p>By default this is not enabled.
   *
   * @return the same instance for method chaining
   * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#lease-semantics">Lease
   * Semantics</a>
   */
  public ChannelConnector lease() {
    return lease((config -> { }));
  }

  /**
   * Enables the Lease feature of the protocol where the number of requests that can be
   * performed from either side are rationed via {@code LEASE} frames from the responder side.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * Mono<Channel> rocketMono =
   *         ChannelConnector.create()
   *                         .lease(spec -> spec.maxPendingRequests(128))
   *                         .connect(transport);
   * }</pre>
   *
   * <p>By default this is not enabled.
   *
   * @param leaseConfigurer consumer which accepts {@link LeaseSpec} and use it for configuring
   * @return the same instance for method chaining
   * @see <a href="https://github.com/today-tech/today-cloud/blob/master/today-remoting/Protocol.md#lease-semantics">Lease
   * Semantics</a>
   */
  public ChannelConnector lease(Consumer<LeaseSpec> leaseConfigurer) {
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
  public ChannelConnector maxInboundPayloadSize(int maxInboundPayloadSize) {
    this.maxInboundPayloadSize = assertInboundPayloadSize(maxInboundPayloadSize);
    return this;
  }

  /**
   * When this is set, frames larger than the given maximum transmission unit (mtu) size value are
   * broken down into fragments to fit that size.
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
  public ChannelConnector fragment(int mtu) {
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
  public ChannelConnector payloadDecoder(PayloadDecoder decoder) {
    Objects.requireNonNull(decoder);
    this.payloadDecoder = decoder;
    return this;
  }

  /**
   * Connect with the given transport and obtain a live {@link Channel} to use for making requests.
   * Each subscriber to the returned {@code Mono} receives a new connection, if neither {@link
   * #reconnect(Retry) reconnect} nor {@link #resume(Resume)} are enabled.
   *
   * @param transport the transport of choice to connect with
   * @return a {@code Mono} with the connected Channel
   */
  public Mono<Channel> connect(ClientTransport transport) {
    return connect(() -> transport);
  }

  /**
   * Variant of {@link #connect(ClientTransport)} with a {@link Supplier} for the {@code
   * ClientTransport}.
   *
   * <p>
   *
   * @param transportSupplier supplier for the transport to connect with
   * @return a {@code Mono} with the connected Channel
   */
  public Mono<Channel> connect(Supplier<ClientTransport> transportSupplier) {
    return Mono.fromSupplier(transportSupplier).flatMap(ct -> {
      int maxFrameLength = ct.getMaxFrameLength();
      Mono<Connection> connectionMono = Mono.fromCallable(() -> {
                assertValidateSetup(maxFrameLength, maxInboundPayloadSize, mtu);
                return ct;
              })
              .flatMap(transport -> transport.connect())
              .map(sourceConnection -> interceptors.initConnection(ConnectionDecorator.Type.SOURCE, sourceConnection))
              .map(con -> LoggingConnection.wrapIfEnabled(con));

      return connectionMono
              .flatMap(connection -> setupPayloadMono
                      .defaultIfEmpty(EmptyPayload.INSTANCE)
                      .map(setupPayload -> Tuples.of(connection, setupPayload))
                      .doOnError(ex -> connection.dispose())
                      .doOnCancel(connection::dispose))
              .flatMap(tuple2 -> {
                Connection sourceConnection = tuple2.getT1();
                Payload setupPayload = tuple2.getT2();
                boolean leaseEnabled = leaseConfigurer != null;
                boolean resumeEnabled = resume != null;
                // TODO: add LeaseClientSetup
                ClientSetup clientSetup = new DefaultClientSetup();
                ByteBuf resumeToken;

                if (resumeEnabled) {
                  resumeToken = resume.tokenGenerator.generate();
                }
                else {
                  resumeToken = Unpooled.EMPTY_BUFFER;
                }

                ByteBuf setupFrame = SetupFrameCodec.encode(sourceConnection.alloc(), leaseEnabled,
                        (int) keepAliveInterval.toMillis(), (int) keepAliveMaxLifeTime.toMillis(),
                        resumeToken, metadataMimeType, dataMimeType, setupPayload);

                sourceConnection.sendFrame(0, setupFrame.retainedSlice());

                return clientSetup.init(sourceConnection).flatMap(tuple -> {
                  final Connection clientServerConnection = tuple.getT2();
                  final Connection wrappedConnection;
                  final KeepAliveHandler keepAliveHandler;

                  if (resumeEnabled) {
                    final var resumableClientSetup = new ResumableClientSetup();
                    final var resumableFramesStore = resume.getStoreFactory(CLIENT_TAG).create(resumeToken);
                    final var resumableConnection = new ResumableConnection(CLIENT_TAG, resumeToken, clientServerConnection, resumableFramesStore);
                    final var session = new ClientChannelSession(resumeToken, resumableConnection, connectionMono, resumableClientSetup::init,
                            resumableFramesStore, resume.sessionDuration, resume.retry, resume.cleanupStoreOnKeepAlive);

                    keepAliveHandler = new KeepAliveHandler.ResumableKeepAliveHandler(resumableConnection, session, session);
                    wrappedConnection = resumableConnection;
                  }
                  else {
                    keepAliveHandler = new KeepAliveHandler.DefaultKeepAliveHandler();
                    wrappedConnection = clientServerConnection;
                  }

                  final InitializingInterceptorRegistry interceptors = this.interceptors;
                  var multiplexer = new ClientServerInputMultiplexer(wrappedConnection, interceptors, true);

                  final LeaseSpec leases;
                  final RequesterLeaseTracker requesterLeaseTracker;
                  if (leaseEnabled) {
                    leases = new LeaseSpec();
                    leaseConfigurer.accept(leases);
                    requesterLeaseTracker = new RequesterLeaseTracker(CLIENT_TAG, leases.maxPendingRequests);
                  }
                  else {
                    leases = null;
                    requesterLeaseTracker = null;
                  }

                  final Sinks.Empty<Void> requesterOnAllClosedSink = Sinks.unsafe().empty();
                  final Sinks.Empty<Void> responderOnAllClosedSink = Sinks.unsafe().empty();

                  Channel channelRequester = new RequesterChannel(multiplexer.asClientConnection(), payloadDecoder,
                          StreamIdProvider.forClient(), mtu, maxFrameLength, maxInboundPayloadSize,
                          (int) keepAliveInterval.toMillis(), (int) keepAliveMaxLifeTime.toMillis(), keepAliveHandler,
                          interceptors::initRequesterRequestInterceptor, requesterLeaseTracker, requesterOnAllClosedSink,
                          Mono.whenDelayError(responderOnAllClosedSink.asMono(), requesterOnAllClosedSink.asMono()));

                  Channel wrappedChannelRequester = interceptors.decorateRequester(channelRequester);
                  ChannelAcceptor acceptor = this.acceptor != null ? this.acceptor : ChannelAcceptor.with(new Channel() { });

                  ConnectionSetupPayload setup = new DefaultConnectionSetupPayload(setupFrame);

                  return interceptors.decorateAcceptor(acceptor)
                          .accept(setup, wrappedChannelRequester)
                          .map(channelHandler -> {
                            Channel wrappedChannelHandler = interceptors.decorateResponder(channelHandler);

                            ResponderLeaseTracker responderLeaseTracker = leaseEnabled
                                    ? new ResponderLeaseTracker(CLIENT_TAG, wrappedConnection, leases.sender)
                                    : null;

                            new ResponderChannel(multiplexer.asServerConnection(), wrappedChannelHandler, payloadDecoder, responderLeaseTracker,
                                    mtu, maxFrameLength, maxInboundPayloadSize, leaseEnabled && leases.sender instanceof TrackingLeaseSender
                                    ? channel -> interceptors.initResponderRequestInterceptor(channel, (TrackingLeaseSender) leases.sender)
                                    : interceptors::initResponderRequestInterceptor, responderOnAllClosedSink);

                            return wrappedChannelRequester;
                          })
                          .doFinally(signalType -> setup.release());
                });
              });
    }).as(source -> {
      if (retrySpec != null) {
        return new ReconnectMono<>(
                source.retryWhen(retrySpec), Closeable::dispose, INVALIDATE_FUNCTION);
      }
      else {
        return source;
      }
    });
  }

  /**
   * Static factory method to create an {@code ChannelConnector} instance and customize default
   * settings before connecting. To connect only, use {@link #connectWith(ClientTransport)}.
   */
  public static ChannelConnector create() {
    return new ChannelConnector();
  }

  /**
   * Static factory method to connect with default settings, effectively a shortcut for:
   *
   * <pre>{@code
   * ChannelConnector.create().connect(transport);
   * }</pre>
   *
   * @param transport the transport of choice to connect with
   * @return a {@code Mono} with the connected Channel
   */
  public static Mono<Channel> connectWith(ClientTransport transport) {
    return ChannelConnector.create().connect(() -> transport);
  }

}
