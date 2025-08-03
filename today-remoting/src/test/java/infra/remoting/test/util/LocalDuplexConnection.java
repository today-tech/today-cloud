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

package infra.remoting.test.util;

import org.reactivestreams.Subscription;

import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import infra.remoting.DuplexConnection;
import infra.remoting.RSocketErrorException;
import infra.remoting.frame.ErrorFrameCodec;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Sinks;

public class LocalDuplexConnection implements DuplexConnection {
  private final ByteBufAllocator allocator;
  private final Sinks.Many<ByteBuf> send;
  private final Sinks.Many<ByteBuf> receive;
  private final Sinks.Empty<Void> onClose;
  private final String name;

  public LocalDuplexConnection(
          String name,
          ByteBufAllocator allocator,
          Sinks.Many<ByteBuf> send,
          Sinks.Many<ByteBuf> receive) {
    this.name = name;
    this.allocator = allocator;
    this.send = send;
    this.receive = receive;
    this.onClose = Sinks.empty();
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    System.out.println(name + " - " + frame.toString());
    send.tryEmitNext(frame);
  }

  @Override
  public void sendErrorAndClose(RSocketErrorException e) {
    final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, 0, e);
    System.out.println(name + " - " + errorFrame.toString());
    send.tryEmitNext(errorFrame);
    onClose.tryEmitEmpty();
  }

  @Override
  public Flux<ByteBuf> receive() {
    return receive
            .asFlux()
            .doOnNext(f -> System.out.println(name + " - " + f.toString()))
            .transform(
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
  public ByteBufAllocator alloc() {
    return allocator;
  }

  @Override
  public SocketAddress remoteAddress() {
    return new TestLocalSocketAddress(name);
  }

  @Override
  public void dispose() {
    onClose.tryEmitEmpty();
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public boolean isDisposed() {
    return onClose.scan(Scannable.Attr.TERMINATED) || onClose.scan(Scannable.Attr.CANCELLED);
  }

  @Override
  public Mono<Void> onClose() {
    return onClose.asMono();
  }
}
