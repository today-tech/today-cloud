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

import java.time.Duration;

import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.buffer.LeaksTrackingByteBufAllocator;
import io.rsocket.transport.ClientTransport;
import reactor.core.publisher.Mono;

import static io.rsocket.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

public class TestClientTransport implements ClientTransport {
  private final LeaksTrackingByteBufAllocator allocator =
          LeaksTrackingByteBufAllocator.instrument(
                  ByteBufAllocator.DEFAULT, Duration.ofSeconds(1), "client");

  private volatile TestDuplexConnection testDuplexConnection;

  int maxFrameLength = FRAME_LENGTH_MASK;

  @Override
  public Mono<DuplexConnection> connect() {
    return Mono.fromSupplier(() -> testDuplexConnection = new TestDuplexConnection(allocator));
  }

  public TestDuplexConnection testConnection() {
    return testDuplexConnection;
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
