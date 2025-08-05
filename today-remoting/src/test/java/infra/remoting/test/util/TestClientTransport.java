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
