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

package io.rsocket.core;

import org.reactivestreams.Publisher;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.rsocket.Channel;
import io.rsocket.DuplexConnection;
import io.rsocket.Payload;
import io.rsocket.exceptions.ConnectionErrorException;
import io.rsocket.frame.ErrorFrameCodec;
import io.rsocket.frame.FrameHeaderCodec;
import io.rsocket.frame.FrameType;
import io.rsocket.frame.RequestChannelFrameCodec;
import io.rsocket.frame.RequestFireAndForgetFrameCodec;
import io.rsocket.frame.RequestNFrameCodec;
import io.rsocket.frame.RequestResponseFrameCodec;
import io.rsocket.frame.RequestStreamFrameCodec;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.plugins.RequestInterceptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Responder side of RSocket. Receives {@link ByteBuf}s from a peer's {@link ChannelRequester}
 */
class ChannelResponder extends RequesterResponderSupport implements Channel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelResponder.class);

  private static final Exception CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

  private final Channel requestHandler;

  private final Sinks.Empty<Void> onThisSideClosedSink;

  @Nullable
  private final ResponderLeaseTracker leaseHandler;

  private volatile Throwable terminationError;
  private static final AtomicReferenceFieldUpdater<ChannelResponder, Throwable> TERMINATION_ERROR =
          AtomicReferenceFieldUpdater.newUpdater(
                  ChannelResponder.class, Throwable.class, "terminationError");

  ChannelResponder(DuplexConnection connection, Channel requestHandler, PayloadDecoder payloadDecoder, @Nullable ResponderLeaseTracker leaseHandler,
          int mtu, int maxFrameLength, int maxInboundPayloadSize, Function<Channel, ? extends RequestInterceptor> requestInterceptorFunction,
          Sinks.Empty<Void> onThisSideClosedSink) {
    super(mtu, maxFrameLength, maxInboundPayloadSize, payloadDecoder, connection, null, requestInterceptorFunction);
    this.leaseHandler = leaseHandler;
    this.requestHandler = requestHandler;
    this.onThisSideClosedSink = onThisSideClosedSink;

    connection.onClose().subscribe(null, this::tryTerminateOnConnectionError, this::tryTerminateOnConnectionClose);

    connection.receive().subscribe(this::handleFrame);
  }

  private void tryTerminateOnConnectionError(Throwable e) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Try terminate connection on responder side");
    }
    tryTerminate(() -> e);
  }

  private void tryTerminateOnConnectionClose() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.info("Try terminate connection on responder side");
    }
    tryTerminate(() -> CLOSED_CHANNEL_EXCEPTION);
  }

  private void tryTerminate(Supplier<Throwable> errorSupplier) {
    if (terminationError == null) {
      Throwable e = errorSupplier.get();
      if (TERMINATION_ERROR.compareAndSet(this, null, e)) {
        doOnDispose();
      }
    }
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    try {
      return requestHandler.fireAndForget(payload);
    }
    catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    try {
      return requestHandler.requestResponse(payload);
    }
    catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    try {
      return requestHandler.requestStream(payload);
    }
    catch (Throwable t) {
      return Flux.error(t);
    }
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    try {
      return requestHandler.requestChannel(payloads);
    }
    catch (Throwable t) {
      return Flux.error(t);
    }
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    try {
      return requestHandler.metadataPush(payload);
    }
    catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public void dispose() {
    tryTerminate(() -> new CancellationException("Disposed"));
  }

  @Override
  public boolean isDisposed() {
    return connection.isDisposed();
  }

  @Override
  public Mono<Void> onClose() {
    return connection.onClose();
  }

  final void doOnDispose() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("closing responder {}", connection);
    }
    cleanupSendingSubscriptions();

    connection.dispose();
    final RequestInterceptor requestInterceptor = getRequestInterceptor();
    if (requestInterceptor != null) {
      requestInterceptor.dispose();
    }

    final ResponderLeaseTracker handler = leaseHandler;
    if (handler != null) {
      handler.dispose();
    }

    requestHandler.dispose();
    onThisSideClosedSink.tryEmitEmpty();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("responder closed {}", connection);
    }
  }

  private void cleanupSendingSubscriptions() {
    final ArrayList<FrameHandler> activeStreamsCopy;
    synchronized(this) {
      activeStreamsCopy = new ArrayList<>(activeStreams.values());
    }

    for (FrameHandler handler : activeStreamsCopy) {
      if (handler != null) {
        handler.handleCancel();
      }
    }
  }

  final void handleFrame(ByteBuf frame) {
    try {
      int streamId = FrameHeaderCodec.streamId(frame);
      FrameHandler receiver;
      FrameType frameType = FrameHeaderCodec.frameType(frame);
      switch (frameType) {
        case REQUEST_FNF:
          handleFireAndForget(streamId, frame);
          break;
        case REQUEST_RESPONSE:
          handleRequestResponse(streamId, frame);
          break;
        case REQUEST_STREAM:
          long streamInitialRequestN = RequestStreamFrameCodec.initialRequestN(frame);
          handleStream(streamId, frame, streamInitialRequestN);
          break;
        case REQUEST_CHANNEL:
          long channelInitialRequestN = RequestChannelFrameCodec.initialRequestN(frame);
          handleChannel(
                  streamId, frame, channelInitialRequestN, FrameHeaderCodec.hasComplete(frame));
          break;
        case METADATA_PUSH:
          handleMetadataPush(metadataPush(getPayloadDecoder().apply(frame)));
          break;
        case CANCEL:
          receiver = get(streamId);
          if (receiver != null) {
            receiver.handleCancel();
          }
          break;
        case REQUEST_N:
          receiver = get(streamId);
          if (receiver != null) {
            long n = RequestNFrameCodec.requestN(frame);
            receiver.handleRequestN(n);
          }
          break;
        case PAYLOAD:
          // TODO: Hook in receiving socket.
          break;
        case NEXT:
          receiver = get(streamId);
          if (receiver != null) {
            boolean hasFollows = FrameHeaderCodec.hasFollows(frame);
            receiver.handleNext(frame, hasFollows, false);
          }
          break;
        case COMPLETE:
          receiver = get(streamId);
          if (receiver != null) {
            receiver.handleComplete();
          }
          break;
        case ERROR:
          receiver = get(streamId);
          if (receiver != null) {
            receiver.handleError(io.rsocket.exceptions.Exceptions.from(streamId, frame));
          }
          break;
        case NEXT_COMPLETE:
          receiver = get(streamId);
          if (receiver != null) {
            receiver.handleNext(frame, false, true);
          }
          break;
        case SETUP:
          connection.sendFrame(streamId, ErrorFrameCodec.encode(allocator, streamId,
                  new IllegalStateException("Setup frame received post setup.")));
          break;
        case LEASE:
        default:
          connection.sendFrame(streamId, ErrorFrameCodec.encode(
                  allocator, streamId, new IllegalStateException("ServerRSocket: Unexpected frame type: " + frameType)));
          break;
      }
    }
    catch (Throwable t) {
      LOGGER.error("Unexpected error during frame handling", t);
      connection.sendFrame(0,
              ErrorFrameCodec.encode(allocator, 0, new ConnectionErrorException("Unexpected error during frame handling", t)));
      tryTerminateOnConnectionError(t);
    }
  }

  final void handleFireAndForget(int streamId, ByteBuf frame) {
    ResponderLeaseTracker leaseHandler = this.leaseHandler;
    Throwable leaseError;
    if (leaseHandler == null || (leaseError = leaseHandler.use()) == null) {
      if (FrameHeaderCodec.hasFollows(frame)) {
        final RequestInterceptor requestInterceptor = getRequestInterceptor();
        if (requestInterceptor != null) {
          requestInterceptor.onStart(streamId, FrameType.REQUEST_FNF, RequestFireAndForgetFrameCodec.metadata(frame));
        }

        FireAndForgetResponderSubscriber subscriber =
                new FireAndForgetResponderSubscriber(streamId, frame, this, this);

        add(streamId, subscriber);
      }
      else {
        final RequestInterceptor requestInterceptor = getRequestInterceptor();
        if (requestInterceptor != null) {
          requestInterceptor.onStart(
                  streamId, FrameType.REQUEST_FNF, RequestFireAndForgetFrameCodec.metadata(frame));

          fireAndForget(getPayloadDecoder().apply(frame))
                  .subscribe(new FireAndForgetResponderSubscriber(streamId, this));
        }
        else {
          fireAndForget(getPayloadDecoder().apply(frame))
                  .subscribe(FireAndForgetResponderSubscriber.INSTANCE);
        }
      }
    }
    else {
      final RequestInterceptor requestTracker = getRequestInterceptor();
      if (requestTracker != null) {
        requestTracker.onReject(leaseError, FrameType.REQUEST_FNF, RequestFireAndForgetFrameCodec.metadata(frame));
      }
    }
  }

  final void handleRequestResponse(int streamId, ByteBuf frame) {
    ResponderLeaseTracker leaseHandler = this.leaseHandler;
    Throwable leaseError;
    if (leaseHandler == null || (leaseError = leaseHandler.use()) == null) {
      final RequestInterceptor requestInterceptor = getRequestInterceptor();
      if (requestInterceptor != null) {
        requestInterceptor.onStart(streamId, FrameType.REQUEST_RESPONSE, RequestResponseFrameCodec.metadata(frame));
      }

      if (FrameHeaderCodec.hasFollows(frame)) {
        var subscriber = new RequestResponseResponderSubscriber(streamId, frame, this, this);
        add(streamId, subscriber);
      }
      else {
        var subscriber = new RequestResponseResponderSubscriber(streamId, this);
        if (add(streamId, subscriber)) {
          requestResponse(getPayloadDecoder().apply(frame)).subscribe(subscriber);
        }
      }
    }
    else {
      final RequestInterceptor requestInterceptor = getRequestInterceptor();
      if (requestInterceptor != null) {
        requestInterceptor.onReject(leaseError, FrameType.REQUEST_RESPONSE, RequestResponseFrameCodec.metadata(frame));
      }
      sendLeaseRejection(streamId, leaseError);
    }
  }

  final void handleStream(int streamId, ByteBuf frame, long initialRequestN) {
    ResponderLeaseTracker leaseHandler = this.leaseHandler;
    Throwable leaseError;
    if (leaseHandler == null || (leaseError = leaseHandler.use()) == null) {
      final RequestInterceptor requestInterceptor = getRequestInterceptor();
      if (requestInterceptor != null) {
        requestInterceptor.onStart(streamId, FrameType.REQUEST_STREAM, RequestStreamFrameCodec.metadata(frame));
      }

      if (FrameHeaderCodec.hasFollows(frame)) {
        var subscriber = new RequestStreamResponderSubscriber(streamId, initialRequestN, frame, this, this);
        add(streamId, subscriber);
      }
      else {
        RequestStreamResponderSubscriber subscriber =
                new RequestStreamResponderSubscriber(streamId, initialRequestN, this);

        if (add(streamId, subscriber)) {
          requestStream(getPayloadDecoder().apply(frame)).subscribe(subscriber);
        }
      }
    }
    else {
      final RequestInterceptor requestInterceptor = getRequestInterceptor();
      if (requestInterceptor != null) {
        requestInterceptor.onReject(
                leaseError, FrameType.REQUEST_STREAM, RequestStreamFrameCodec.metadata(frame));
      }
      sendLeaseRejection(streamId, leaseError);
    }
  }

  final void handleChannel(int streamId, ByteBuf frame, long initialRequestN, boolean complete) {
    ResponderLeaseTracker leaseHandler = this.leaseHandler;
    Throwable leaseError;
    if (leaseHandler == null || (leaseError = leaseHandler.use()) == null) {
      final RequestInterceptor requestInterceptor = getRequestInterceptor();
      if (requestInterceptor != null) {
        requestInterceptor.onStart(streamId, FrameType.REQUEST_CHANNEL, RequestChannelFrameCodec.metadata(frame));
      }

      if (FrameHeaderCodec.hasFollows(frame)) {
        RequestChannelResponderSubscriber subscriber =
                new RequestChannelResponderSubscriber(streamId, initialRequestN, frame, this, this);

        add(streamId, subscriber);
      }
      else {
        final Payload firstPayload = getPayloadDecoder().apply(frame);
        RequestChannelResponderSubscriber subscriber =
                new RequestChannelResponderSubscriber(streamId, initialRequestN, firstPayload, this);

        if (add(streamId, subscriber)) {
          requestChannel(subscriber).subscribe(subscriber);
          if (complete) {
            subscriber.handleComplete();
          }
        }
      }
    }
    else {
      final RequestInterceptor requestTracker = getRequestInterceptor();
      if (requestTracker != null) {
        requestTracker.onReject(leaseError, FrameType.REQUEST_CHANNEL, RequestChannelFrameCodec.metadata(frame));
      }
      sendLeaseRejection(streamId, leaseError);
    }
  }

  private void sendLeaseRejection(int streamId, Throwable leaseError) {
    connection.sendFrame(streamId, ErrorFrameCodec.encode(getAllocator(), streamId, leaseError));
  }

  private void handleMetadataPush(Mono<Void> result) {
    result.subscribe(MetadataPushResponderSubscriber.INSTANCE);
  }

  @Override
  public boolean add(int streamId, FrameHandler frameHandler) {
    if (!super.add(streamId, frameHandler)) {
      frameHandler.handleCancel();
      return false;
    }

    return true;
  }
}
