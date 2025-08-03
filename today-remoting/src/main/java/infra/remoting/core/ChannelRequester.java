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

import org.reactivestreams.Publisher;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.DuplexConnection;
import infra.remoting.Payload;
import infra.remoting.exceptions.ConnectionErrorException;
import infra.remoting.exceptions.Exceptions;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.RequestNFrameCodec;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.keepalive.KeepAliveFramesAcceptor;
import infra.remoting.keepalive.KeepAliveHandler;
import infra.remoting.keepalive.KeepAliveSupport;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.buffer.ByteBuf;
import io.netty.util.collection.IntObjectMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import static infra.remoting.keepalive.KeepAliveSupport.ClientKeepAliveSupport;

/**
 * Requester Side of a Channel socket. Sends {@link ByteBuf}s to a {@link ChannelResponder} of peer
 */
class ChannelRequester extends ChannelSupport implements Channel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelRequester.class);

  private static final Exception CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

  static {
    CLOSED_CHANNEL_EXCEPTION.setStackTrace(new StackTraceElement[0]);
  }

  private volatile Throwable terminationError;
  private static final AtomicReferenceFieldUpdater<ChannelRequester, Throwable> TERMINATION_ERROR =
          AtomicReferenceFieldUpdater.newUpdater(ChannelRequester.class, Throwable.class, "terminationError");

  @Nullable
  private final RequesterLeaseTracker requesterLeaseTracker;

  private final Sinks.Empty<Void> onThisSideClosedSink;

  private final KeepAliveFramesAcceptor keepAliveFramesAcceptor;

  private final Mono<Void> onAllClosed;

  ChannelRequester(DuplexConnection connection, PayloadDecoder payloadDecoder, StreamIdProvider streamIdProvider,
          int mtu, int maxFrameLength, int maxInboundPayloadSize, int keepAliveTickPeriod, int keepAliveAckTimeout,
          @Nullable KeepAliveHandler keepAliveHandler, Function<Channel, RequestInterceptor> requestInterceptorFunction,
          @Nullable RequesterLeaseTracker requesterLeaseTracker, Sinks.Empty<Void> onThisSideClosedSink, Mono<Void> onAllClosed) {
    super(mtu, maxFrameLength, maxInboundPayloadSize, payloadDecoder, connection, streamIdProvider, requestInterceptorFunction);

    this.requesterLeaseTracker = requesterLeaseTracker;
    this.onThisSideClosedSink = onThisSideClosedSink;
    this.onAllClosed = onAllClosed;

    // DO NOT Change the order here. The Send processor must be subscribed to before receiving
    connection.onClose().subscribe(null, this::tryShutdown, this::tryShutdown);

    connection.receive().subscribe(this::handleIncomingFrames);

    if (keepAliveTickPeriod != 0 && keepAliveHandler != null) {
      KeepAliveSupport keepAliveSupport = new ClientKeepAliveSupport(allocator, keepAliveTickPeriod, keepAliveAckTimeout);
      this.keepAliveFramesAcceptor = keepAliveHandler.start(keepAliveSupport,
              keepAliveFrame -> connection.sendFrame(0, keepAliveFrame), this::tryTerminateOnKeepAlive);
    }
    else {
      keepAliveFramesAcceptor = null;
    }
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    if (this.requesterLeaseTracker == null) {
      return new FireAndForgetRequesterMono(payload, this);
    }
    else {
      return new SlowFireAndForgetRequesterMono(payload, this);
    }
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    return new RequestResponseRequesterMono(payload, this);
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    return new RequestStreamRequesterFlux(payload, this);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return new RequestChannelRequesterFlux(payloads, this);
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    Throwable terminationError = this.terminationError;
    if (terminationError != null) {
      payload.release();
      return Mono.error(terminationError);
    }

    return new MetadataPushRequesterMono(payload, this);
  }

  @Nullable
  @Override
  public RequesterLeaseTracker getRequesterLeaseTracker() {
    return this.requesterLeaseTracker;
  }

  @Override
  public int getNextStreamId() {
    int nextStreamId = super.getNextStreamId();

    Throwable terminationError = this.terminationError;
    if (terminationError != null) {
      throw reactor.core.Exceptions.propagate(terminationError);
    }

    return nextStreamId;
  }

  @Override
  public int addAndGetNextStreamId(FrameHandler frameHandler) {
    int nextStreamId = super.addAndGetNextStreamId(frameHandler);

    Throwable terminationError = this.terminationError;
    if (terminationError != null) {
      super.remove(nextStreamId, frameHandler);
      throw reactor.core.Exceptions.propagate(terminationError);
    }

    return nextStreamId;
  }

  @Override
  public double availability() {
    final RequesterLeaseTracker requesterLeaseTracker = this.requesterLeaseTracker;
    if (requesterLeaseTracker != null) {
      return Math.min(connection.availability(), requesterLeaseTracker.availability());
    }
    else {
      return connection.availability();
    }
  }

  @Override
  public void dispose() {
    if (terminationError != null) {
      return;
    }

    connection.sendErrorAndClose(new ConnectionErrorException("Disposed"));
  }

  @Override
  public boolean isDisposed() {
    return terminationError != null;
  }

  @Override
  public Mono<Void> onClose() {
    return onAllClosed;
  }

  private void handleIncomingFrames(ByteBuf frame) {
    try {
      int streamId = FrameHeaderCodec.streamId(frame);
      FrameType type = FrameHeaderCodec.frameType(frame);
      if (streamId == 0) {
        handleStreamZero(type, frame);
      }
      else {
        handleFrame(streamId, type, frame);
      }
    }
    catch (Throwable t) {
      LOGGER.error("Unexpected error during frame handling", t);
      final ConnectionErrorException error = new ConnectionErrorException("Unexpected error during frame handling", t);
      connection.sendErrorAndClose(error);
    }
  }

  private void handleStreamZero(FrameType type, ByteBuf frame) {
    switch (type) {
      case ERROR:
        tryTerminateOnZeroError(frame);
        break;
      case LEASE:
        requesterLeaseTracker.handleLeaseFrame(frame);
        break;
      case KEEPALIVE:
        if (keepAliveFramesAcceptor != null) {
          keepAliveFramesAcceptor.receive(frame);
        }
        break;
      default:
        // Ignore unknown frames. Throwing an error will close the socket.
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Requester received unsupported frame on stream 0: {}", frame.toString());
        }
    }
  }

  private void handleFrame(int streamId, FrameType type, ByteBuf frame) {
    FrameHandler receiver = get(streamId);
    if (receiver == null) {
      handleMissingResponseProcessor(streamId, type, frame);
      return;
    }

    switch (type) {
      case NEXT_COMPLETE:
        receiver.handleNext(frame, false, true);
        break;
      case NEXT:
        boolean hasFollows = FrameHeaderCodec.hasFollows(frame);
        receiver.handleNext(frame, hasFollows, false);
        break;
      case COMPLETE:
        receiver.handleComplete();
        break;
      case ERROR:
        receiver.handleError(Exceptions.from(streamId, frame));
        break;
      case CANCEL:
        receiver.handleCancel();
        break;
      case REQUEST_N:
        long n = RequestNFrameCodec.requestN(frame);
        receiver.handleRequestN(n);
        break;
      default:
        throw new IllegalStateException(
                "Requester received unsupported frame on stream %d: %s".formatted(streamId, frame.toString()));
    }
  }

  @SuppressWarnings("ConstantConditions")
  private void handleMissingResponseProcessor(int streamId, FrameType type, ByteBuf frame) {
    if (!super.streamIdProvider.isBeforeOrCurrent(streamId)) {
      if (type == FrameType.ERROR) {
        // message for stream that has never existed, we have a problem with
        // the overall connection and must tear down
        String errorMessage = ErrorFrameCodec.dataUtf8(frame);
        throw new IllegalStateException(
                "Client received error for non-existent stream: %d Message: %s".formatted(streamId, errorMessage));
      }
      else {
        throw new IllegalStateException(
                "Client received message for non-existent stream: %d, frame type: %s".formatted(streamId, type));
      }
    }
    // receiving a frame after a given stream has been cancelled/completed,
    // so ignore (cancellation is async so there is a race condition)
  }

  private void tryTerminateOnKeepAlive(KeepAliveSupport.KeepAlive keepAlive) {
    tryTerminate(() -> new ConnectionErrorException(
            String.format("No keep-alive acks for %d ms", keepAlive.getTimeout().toMillis())));
    connection.dispose();
  }

  private void tryShutdown(Throwable e) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("trying to close requester {}", connection);
    }
    if (terminationError == null) {
      if (TERMINATION_ERROR.compareAndSet(this, null, e)) {
        terminate(CLOSED_CHANNEL_EXCEPTION);
      }
      else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("trying to close requester failed because of {} {}", terminationError, connection);
        }
      }
    }
    else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.info("trying to close requester failed because of {} {}", terminationError, connection);
      }
    }
  }

  private void tryTerminateOnZeroError(ByteBuf errorFrame) {
    tryTerminate(() -> Exceptions.from(0, errorFrame));
  }

  private void tryTerminate(Supplier<Throwable> errorSupplier) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("trying to close requester {}", connection);
    }
    if (terminationError == null) {
      Throwable e = errorSupplier.get();
      if (TERMINATION_ERROR.compareAndSet(this, null, e)) {
        terminate(e);
      }
      else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("trying to close requester failed because of {} {}", terminationError, connection);
        }
      }
    }
    else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("trying to close requester failed because of {} {}", terminationError, connection);
      }
    }
  }

  private void tryShutdown() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("trying to close requester {}", connection);
    }
    if (terminationError == null) {
      if (TERMINATION_ERROR.compareAndSet(this, null, CLOSED_CHANNEL_EXCEPTION)) {
        terminate(CLOSED_CHANNEL_EXCEPTION);
      }
      else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("trying to close requester failed because of {} {}", terminationError, connection);
        }
      }
    }
    else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("trying to close requester failed because of {} {}", terminationError, connection);
      }
    }
  }

  private void terminate(Throwable e) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("closing requester {} due to {}", connection, e);
    }
    if (keepAliveFramesAcceptor != null) {
      keepAliveFramesAcceptor.dispose();
    }
    final RequestInterceptor requestInterceptor = getRequestInterceptor();
    if (requestInterceptor != null) {
      requestInterceptor.dispose();
    }

    final RequesterLeaseTracker requesterLeaseTracker = this.requesterLeaseTracker;
    if (requesterLeaseTracker != null) {
      requesterLeaseTracker.dispose(e);
    }

    final ArrayList<FrameHandler> activeStreamsCopy;
    synchronized(this) {
      final IntObjectMap<FrameHandler> activeStreams = this.activeStreams;
      activeStreamsCopy = new ArrayList<>(activeStreams.values());
    }

    for (FrameHandler handler : activeStreamsCopy) {
      if (handler != null) {
        try {
          handler.handleError(e);
        }
        catch (Throwable ignored) {
        }
      }
    }

    if (e == CLOSED_CHANNEL_EXCEPTION) {
      onThisSideClosedSink.tryEmitEmpty();
    }
    else {
      onThisSideClosedSink.tryEmitError(e);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("requester closed {}", connection);
    }
  }
}
