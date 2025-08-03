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

import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Closeable;
import io.rsocket.buffer.LeaksTrackingByteBufAllocator;
import io.rsocket.transport.ConnectionAcceptor;
import io.rsocket.transport.ServerTransport;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

public class TestServerTransport implements ServerTransport<Closeable> {
  private final Sinks.One<TestDuplexConnection> connSink = Sinks.one();
  private TestDuplexConnection connection;
  private final LeaksTrackingByteBufAllocator allocator =
          LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);

  int maxFrameLength = FRAME_LENGTH_MASK;

  @Override
  public Mono<Closeable> start(ConnectionAcceptor acceptor) {
    connSink
            .asMono()
            .flatMap(duplexConnection -> acceptor.accept(duplexConnection))
            .subscribe(ignored -> { }, err -> disposeConnection(), this::disposeConnection);
    return Mono.just(
            new Closeable() {
              @Override
              public Mono<Void> onClose() {
                return connSink.asMono().then();
              }

              @Override
              public void dispose() {
                connSink.tryEmitEmpty();
              }

              @Override
              @SuppressWarnings("ConstantConditions")
              public boolean isDisposed() {
                return connSink.scan(Scannable.Attr.TERMINATED)
                        || connSink.scan(Scannable.Attr.CANCELLED);
              }
            });
  }

  private void disposeConnection() {
    TestDuplexConnection c = connection;
    if (c != null) {
      c.dispose();
    }
  }

  public TestDuplexConnection connect() {
    TestDuplexConnection c = new TestDuplexConnection(allocator);
    connection = c;
    connSink.tryEmitValue(c);
    return c;
  }

  public LeaksTrackingByteBufAllocator alloc() {
    return allocator;
  }

  public TestServerTransport withMaxFrameLength(int maxFrameLength) {
    this.maxFrameLength = maxFrameLength;
    return this;
  }

  @Override
  public int getMaxFrameLength() {
    return maxFrameLength;
  }
}
