/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package infra.remoting.test.util;

import infra.remoting.Closeable;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.transport.ConnectionAcceptor;
import infra.remoting.transport.ServerTransport;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

public class TestServerTransport implements ServerTransport<Closeable> {
  private final Sinks.One<TestConnection> connSink = Sinks.one();
  private TestConnection connection;
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
    TestConnection c = connection;
    if (c != null) {
      c.dispose();
    }
  }

  public TestConnection connect() {
    TestConnection c = new TestConnection(allocator);
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
