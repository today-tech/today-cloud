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

package io.rsocket.test;

import org.reactivestreams.Subscription;

import java.net.SocketAddress;
import java.util.function.BiFunction;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketErrorException;
import io.rsocket.frame.PayloadFrameCodec;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Sinks;

public class TestDuplexConnection implements DuplexConnection {

  final ByteBufAllocator allocator;
  final Sinks.Many<ByteBuf> inbound = Sinks.unsafe().many().unicast().onBackpressureError();
  final Sinks.Many<ByteBuf> outbound = Sinks.unsafe().many().unicast().onBackpressureError();
  final Sinks.One<Void> close = Sinks.one();

  public TestDuplexConnection(
          CoreSubscriber<? super ByteBuf> outboundSubscriber, boolean trackLeaks) {
    this.outbound.asFlux().subscribe(outboundSubscriber);
    this.allocator =
            trackLeaks
                    ? LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT)
                    : ByteBufAllocator.DEFAULT;
  }

  @Override
  public void dispose() {
    this.inbound.tryEmitComplete();
    this.outbound.tryEmitComplete();
    this.close.tryEmitEmpty();
  }

  @Override
  public Mono<Void> onClose() {
    return this.close.asMono();
  }

  @Override
  public void sendErrorAndClose(RSocketErrorException errorException) { }

  @Override
  public Flux<ByteBuf> receive() {
    return this.inbound
            .asFlux()
            .transform(
                    Operators.lift(
                            (BiFunction<
                                    Scannable,
                                    CoreSubscriber<? super ByteBuf>,
                                    CoreSubscriber<? super ByteBuf>>)
                                    ByteBufReleaserOperator::create));
  }

  @Override
  public ByteBufAllocator alloc() {
    return this.allocator;
  }

  @Override
  public SocketAddress remoteAddress() {
    return new SocketAddress() {
      @Override
      public String toString() {
        return "Test";
      }
    };
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    this.outbound.tryEmitNext(frame);
  }

  public void sendPayloadFrame(
          int streamId, ByteBuf data, @Nullable ByteBuf metadata, boolean complete) {
    sendFrame(
            streamId,
            PayloadFrameCodec.encode(this.allocator, streamId, false, complete, true, metadata, data));
  }

  static class ByteBufReleaserOperator
          implements CoreSubscriber<ByteBuf>, Subscription, Fuseable.QueueSubscription<ByteBuf> {

    static CoreSubscriber<? super ByteBuf> create(
            Scannable scannable, CoreSubscriber<? super ByteBuf> actual) {
      return new ByteBufReleaserOperator(actual);
    }

    final CoreSubscriber<? super ByteBuf> actual;

    Subscription s;

    public ByteBufReleaserOperator(CoreSubscriber<? super ByteBuf> actual) {
      this.actual = actual;
    }

    @Override
    public void onSubscribe(Subscription s) {
      if (Operators.validate(this.s, s)) {
        this.s = s;
        this.actual.onSubscribe(this);
      }
    }

    @Override
    public void onNext(ByteBuf buf) {
      this.actual.onNext(buf);
      buf.release();
    }

    @Override
    public void onError(Throwable t) {
      actual.onError(t);
    }

    @Override
    public void onComplete() {
      actual.onComplete();
    }

    @Override
    public void request(long n) {
      s.request(n);
    }

    @Override
    public void cancel() {
      s.cancel();
    }

    @Override
    public int requestFusion(int requestedMode) {
      return Fuseable.NONE;
    }

    @Override
    public ByteBuf poll() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public int size() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public boolean isEmpty() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }
  }
}
