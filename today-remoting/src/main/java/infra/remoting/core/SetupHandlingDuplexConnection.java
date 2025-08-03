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

import org.reactivestreams.Subscription;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;

import infra.remoting.DuplexConnection;
import infra.remoting.ProtocolErrorException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

class SetupHandlingDuplexConnection extends Flux<ByteBuf>
        implements DuplexConnection, CoreSubscriber<ByteBuf>, Subscription {

  final DuplexConnection source;
  final MonoSink<Tuple2<ByteBuf, DuplexConnection>> sink;

  Subscription s;
  boolean firstFrameReceived = false;

  CoreSubscriber<? super ByteBuf> actual;

  boolean done;
  Throwable t;

  SetupHandlingDuplexConnection(
          DuplexConnection source, MonoSink<Tuple2<ByteBuf, DuplexConnection>> sink) {
    this.source = source;
    this.sink = sink;

    source.receive().subscribe(this);
  }

  @Override
  public void dispose() {
    source.dispose();
  }

  @Override
  public boolean isDisposed() {
    return source.isDisposed();
  }

  @Override
  public Mono<Void> onClose() {
    return source.onClose();
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    source.sendFrame(streamId, frame);
  }

  @Override
  public Flux<ByteBuf> receive() {
    return this;
  }

  @Override
  public SocketAddress remoteAddress() {
    return source.remoteAddress();
  }

  @Override
  public void subscribe(CoreSubscriber<? super ByteBuf> actual) {
    if (done) {
      final Throwable t = this.t;
      if (t == null) {
        Operators.complete(actual);
      }
      else {
        Operators.error(actual, t);
      }
      return;
    }

    this.actual = actual;
    actual.onSubscribe(this);
  }

  @Override
  public void request(long n) {
    if (n != Long.MAX_VALUE) {
      actual.onError(new IllegalArgumentException("Only unbounded request is allowed"));
      return;
    }

    s.request(Long.MAX_VALUE);
  }

  @Override
  public void cancel() {
    source.dispose();
    s.cancel();
  }

  @Override
  public void onSubscribe(Subscription s) {
    if (Operators.validate(this.s, s)) {
      this.s = s;
      s.request(1);
    }
  }

  @Override
  public void onNext(ByteBuf frame) {
    if (!firstFrameReceived) {
      firstFrameReceived = true;
      sink.success(Tuples.of(frame, this));
      return;
    }

    actual.onNext(frame);
  }

  @Override
  public void onError(Throwable t) {
    if (done) {
      Operators.onErrorDropped(t, Context.empty());
      return;
    }

    this.done = true;
    this.t = t;

    if (!firstFrameReceived) {
      sink.error(t);
      return;
    }

    final CoreSubscriber<? super ByteBuf> actual = this.actual;
    if (actual != null) {
      actual.onError(t);
    }
  }

  @Override
  public void onComplete() {
    if (done) {
      return;
    }

    this.done = true;

    if (!firstFrameReceived) {
      sink.error(new ClosedChannelException());
      return;
    }

    final CoreSubscriber<? super ByteBuf> actual = this.actual;
    if (actual != null) {
      actual.onComplete();
    }
  }

  @Override
  public void sendErrorAndClose(ProtocolErrorException e) {
    source.sendErrorAndClose(e);
  }

  @Override
  public ByteBufAllocator alloc() {
    return source.alloc();
  }

  @Override
  public String toString() {
    return "SetupHandlingDuplexConnection{" + "source=" + source + ", done=" + done + '}';
  }
}
