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

import java.time.Duration;

import infra.remoting.Connection;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.transport.ClientTransport;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

public class TestClientTransport implements ClientTransport {

  private final LeaksTrackingByteBufAllocator allocator =
          LeaksTrackingByteBufAllocator.instrument(
                  ByteBufAllocator.DEFAULT, Duration.ofSeconds(1), "client");

  private volatile TestConnection testConnection;

  int maxFrameLength = FRAME_LENGTH_MASK;

  @Override
  public Mono<Connection> connect() {
    return Mono.fromSupplier(() -> testConnection = new TestConnection(allocator));
  }

  public TestConnection testConnection() {
    return testConnection;
  }

  public LeaksTrackingByteBufAllocator alloc() {
    return allocator;
  }

  public TestClientTransport withMaxFrameLength(int maxFrameLength) {
    this.maxFrameLength = maxFrameLength;
    return this;
  }

  @Override
  public int getMaxFrameLength() {
    return maxFrameLength;
  }
}
