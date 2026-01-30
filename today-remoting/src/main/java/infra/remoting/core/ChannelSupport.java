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

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

import infra.remoting.Channel;
import infra.remoting.Connection;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

class ChannelSupport implements Channel {

  public final int mtu;

  public final int maxFrameLength;

  public final int maxInboundPayloadSize;

  public final PayloadDecoder payloadDecoder;

  public final ByteBufAllocator allocator;

  public final Connection connection;

  @Nullable
  public final RequestInterceptor requestInterceptor;

  @Nullable
  protected final StreamIdProvider streamIdProvider;

  protected final IntObjectMap<FrameHandler> activeStreams;

  public ChannelSupport(int mtu, int maxFrameLength, int maxInboundPayloadSize,
          PayloadDecoder payloadDecoder, Connection connection, @Nullable StreamIdProvider streamIdProvider,
          Function<Channel, ? extends RequestInterceptor> requestInterceptorFunction) {

    this.activeStreams = new IntObjectHashMap<>();
    this.mtu = mtu;
    this.maxFrameLength = maxFrameLength;
    this.maxInboundPayloadSize = maxInboundPayloadSize;
    this.payloadDecoder = payloadDecoder;
    this.allocator = connection.alloc();
    this.streamIdProvider = streamIdProvider;
    this.connection = connection;
    this.requestInterceptor = requestInterceptorFunction.apply(this);
  }

  public int getMtu() {
    return mtu;
  }

  public int getMaxFrameLength() {
    return maxFrameLength;
  }

  public int getMaxInboundPayloadSize() {
    return maxInboundPayloadSize;
  }

  public PayloadDecoder getPayloadDecoder() {
    return payloadDecoder;
  }

  public ByteBufAllocator getAllocator() {
    return allocator;
  }

  public Connection getConnection() {
    return connection;
  }

  @Nullable
  public RequesterLeaseTracker getRequesterLeaseTracker() {
    return null;
  }

  @Nullable
  public RequestInterceptor getRequestInterceptor() {
    return requestInterceptor;
  }

  /**
   * Issues next {@code streamId}
   *
   * @return issued {@code streamId}
   * @throws RuntimeException if the {@link ChannelSupport} is terminated for any reason
   */
  public int getNextStreamId() {
    final StreamIdProvider streamIdProvider = this.streamIdProvider;
    if (streamIdProvider != null) {
      synchronized(this) {
        return streamIdProvider.nextStreamId(this.activeStreams);
      }
    }
    else {
      throw new UnsupportedOperationException("Responder can not issue id");
    }
  }

  /**
   * Adds frameHandler and returns issued {@code streamId} back
   *
   * @param frameHandler to store
   * @return issued {@code streamId}
   * @throws RuntimeException if the {@link ChannelSupport} is terminated for any reason
   */
  public int addAndGetNextStreamId(FrameHandler frameHandler) {
    final StreamIdProvider streamIdProvider = this.streamIdProvider;
    if (streamIdProvider != null) {
      final IntObjectMap<FrameHandler> activeStreams = this.activeStreams;
      synchronized(this) {
        final int streamId = streamIdProvider.nextStreamId(activeStreams);
        activeStreams.put(streamId, frameHandler);
        return streamId;
      }
    }
    else {
      throw new UnsupportedOperationException("Responder can not issue id");
    }
  }

  public synchronized boolean add(int streamId, FrameHandler frameHandler) {
    final IntObjectMap<FrameHandler> activeStreams = this.activeStreams;
    // copy of Map.putIfAbsent(key, value) without `streamId` boxing
    final FrameHandler previousHandler = activeStreams.get(streamId);
    if (previousHandler == null) {
      activeStreams.put(streamId, frameHandler);
      return true;
    }
    return false;
  }

  /**
   * Resolves {@link FrameHandler} by {@code streamId}
   *
   * @param streamId used to resolve {@link FrameHandler}
   * @return {@link FrameHandler} or {@code null}
   */
  @Nullable
  public synchronized FrameHandler get(int streamId) {
    return this.activeStreams.get(streamId);
  }

  /**
   * Removes {@link FrameHandler} if it is present and equals to the given one
   *
   * @param streamId to lookup for {@link FrameHandler}
   * @param frameHandler instance to check with the found one
   * @return {@code true} if there is {@link FrameHandler} for the given {@code streamId} and the
   * instance equals to the passed one
   */
  public synchronized boolean remove(int streamId, FrameHandler frameHandler) {
    final IntObjectMap<FrameHandler> activeStreams = this.activeStreams;
    // copy of Map.remove(key, value) without `streamId` boxing
    final FrameHandler curValue = activeStreams.get(streamId);
    if (!Objects.equals(curValue, frameHandler)) {
      return false;
    }
    activeStreams.remove(streamId);
    return true;
  }
}
