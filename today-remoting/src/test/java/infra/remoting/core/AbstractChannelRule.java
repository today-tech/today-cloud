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

package infra.remoting.core;

import org.reactivestreams.Subscriber;

import java.time.Duration;

import infra.remoting.Channel;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.test.util.TestConnection;
import infra.remoting.test.util.TestSubscriber;
import io.netty.buffer.ByteBufAllocator;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

public abstract class AbstractChannelRule<T extends Channel> {

  protected TestConnection connection;
  protected Subscriber<Void> connectSub;
  protected T channel;
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
    if (channel != null) {
      channel.dispose();
    }
    connection = new TestConnection(allocator);
    channel = newChannel();
  }

  public void setMaxInboundPayloadSize(int maxInboundPayloadSize) {
    this.maxInboundPayloadSize = maxInboundPayloadSize;
    doInit();
  }

  public void setMaxFrameLength(int maxFrameLength) {
    this.maxFrameLength = maxFrameLength;
    doInit();
  }

  protected abstract T newChannel();

  public LeaksTrackingByteBufAllocator alloc() {
    return allocator;
  }

  public void assertHasNoLeaks() {
    allocator.assertHasNoLeaks();
  }
}
