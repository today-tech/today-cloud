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

import org.reactivestreams.Subscriber;

import java.time.Duration;

import io.netty.buffer.ByteBufAllocator;
import infra.remoting.Channel;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.test.util.TestDuplexConnection;
import infra.remoting.test.util.TestSubscriber;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

public abstract class AbstractSocketRule<T extends Channel> {

  protected TestDuplexConnection connection;
  protected Subscriber<Void> connectSub;
  protected T socket;
  protected LeaksTrackingByteBufAllocator allocator;
  protected int maxFrameLength = FRAME_LENGTH_MASK;
  protected int maxInboundPayloadSize = Integer.MAX_VALUE;

  public void init() {
    allocator =
            LeaksTrackingByteBufAllocator.instrument(
                    ByteBufAllocator.DEFAULT, Duration.ofSeconds(5), "");
    connectSub = TestSubscriber.create();
    doInit();
  }

  protected void doInit() {
    if (connection != null) {
      connection.dispose();
    }
    if (socket != null) {
      socket.dispose();
    }
    connection = new TestDuplexConnection(allocator);
    socket = newRSocket();
  }

  public void setMaxInboundPayloadSize(int maxInboundPayloadSize) {
    this.maxInboundPayloadSize = maxInboundPayloadSize;
    doInit();
  }

  public void setMaxFrameLength(int maxFrameLength) {
    this.maxFrameLength = maxFrameLength;
    doInit();
  }

  protected abstract T newRSocket();

  public LeaksTrackingByteBufAllocator alloc() {
    return allocator;
  }

  public void assertHasNoLeaks() {
    allocator.assertHasNoLeaks();
  }
}
