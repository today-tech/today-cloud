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

import java.util.Objects;
import java.util.function.Function;

import infra.lang.Nullable;
import infra.remoting.Channel;
import infra.remoting.DuplexConnection;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

class ChannelSupport {

  public final int mtu;

  public final int maxFrameLength;

  public final int maxInboundPayloadSize;

  public final PayloadDecoder payloadDecoder;

  public final ByteBufAllocator allocator;

  public final DuplexConnection connection;

  @Nullable
  public final RequestInterceptor requestInterceptor;

  @Nullable
  protected final StreamIdProvider streamIdProvider;

  protected final IntObjectMap<FrameHandler> activeStreams;

  public ChannelSupport(int mtu, int maxFrameLength, int maxInboundPayloadSize,
          PayloadDecoder payloadDecoder, DuplexConnection connection, @Nullable StreamIdProvider streamIdProvider,
          Function<Channel, ? extends RequestInterceptor> requestInterceptorFunction) {

    this.activeStreams = new IntObjectHashMap<>();
    this.mtu = mtu;
    this.maxFrameLength = maxFrameLength;
    this.maxInboundPayloadSize = maxInboundPayloadSize;
    this.payloadDecoder = payloadDecoder;
    this.allocator = connection.alloc();
    this.streamIdProvider = streamIdProvider;
    this.connection = connection;
    this.requestInterceptor = requestInterceptorFunction.apply((Channel) this);
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

  public DuplexConnection getDuplexConnection() {
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
