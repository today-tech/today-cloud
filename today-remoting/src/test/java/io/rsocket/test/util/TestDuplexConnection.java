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

package io.rsocket.test.util;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import infra.lang.NonNull;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketErrorException;
import io.rsocket.buffer.LeaksTrackingByteBufAllocator;
import io.rsocket.frame.ErrorFrameCodec;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.Operators;

/**
 * An implementation of {@link DuplexConnection} that provides functionality to modify the behavior
 * dynamically.
 */
public class TestDuplexConnection implements DuplexConnection {

  private static final Logger logger = LoggerFactory.getLogger(TestDuplexConnection.class);

  private final LinkedBlockingQueue<ByteBuf> sent;

  private final DirectProcessor<ByteBuf> sentPublisher;
  private final FluxSink<ByteBuf> sendSink;
  private final DirectProcessor<ByteBuf> received;
  private final FluxSink<ByteBuf> receivedSink;
  private final MonoProcessor<Void> onClose;
  private final LeaksTrackingByteBufAllocator allocator;
  private volatile double availability = 1;
  private volatile int initialSendRequestN = Integer.MAX_VALUE;

  public TestDuplexConnection(LeaksTrackingByteBufAllocator allocator) {
    this.allocator = allocator;
    this.sent = new LinkedBlockingQueue<>();
    this.received = DirectProcessor.create();
    this.receivedSink = received.sink();
    this.sentPublisher = DirectProcessor.create();
    this.sendSink = sentPublisher.sink();
    this.onClose = MonoProcessor.create();
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    if (availability <= 0) {
      throw new IllegalStateException("RSocket not available. Availability: " + availability);
    }

    sendSink.next(frame);
    sent.offer(frame);
  }

  @Override
  public Flux<ByteBuf> receive() {
    return received.transform(
            Operators.<ByteBuf, ByteBuf>lift(
                    (__, actual) ->
                            new CoreSubscriber<ByteBuf>() {
                              @Override
                              public void onSubscribe(Subscription s) {
                                actual.onSubscribe(s);
                              }

                              @Override
                              public void onNext(ByteBuf byteBuf) {
                                actual.onNext(byteBuf);
                                byteBuf.release();
                              }

                              @Override
                              public void onError(Throwable t) {
                                actual.onError(t);
                              }

                              @Override
                              public void onComplete() {
                                actual.onComplete();
                              }
                            }));
  }

  @Override
  public void sendErrorAndClose(RSocketErrorException e) {
    final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, 0, e);
    sendSink.next(errorFrame);
    sent.offer(errorFrame);

    final Throwable cause = e.getCause();
    if (cause == null) {
      onClose.onComplete();
    }
    else {
      onClose.onError(cause);
    }
  }

  @Override
  public LeaksTrackingByteBufAllocator alloc() {
    return allocator;
  }

  @Override
  public SocketAddress remoteAddress() {
    return new TestLocalSocketAddress("TestDuplexConnection");
  }

  @Override
  public double availability() {
    return availability;
  }

  @Override
  public void dispose() {
    onClose.onComplete();
  }

  @Override
  public boolean isDisposed() {
    return onClose.isDisposed();
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }

  public boolean isEmpty() {
    return sent.isEmpty();
  }

  @NonNull
  public ByteBuf awaitFrame() {
    try {
      return sent.take();
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public ByteBuf pollFrame() {
    return sent.poll();
  }

  public void setAvailability(double availability) {
    this.availability = availability;
  }

  public BlockingQueue<ByteBuf> getSent() {
    return sent;
  }

  public Publisher<ByteBuf> getSentAsPublisher() {
    return sentPublisher;
  }

  public void addToReceivedBuffer(ByteBuf... received) {
    for (ByteBuf frame : received) {
      this.receivedSink.next(frame);
    }
  }

  public void clearSendReceiveBuffers() {
    sent.clear();
  }

  public void setInitialSendRequestN(int initialSendRequestN) {
    this.initialSendRequestN = initialSendRequestN;
  }
}
