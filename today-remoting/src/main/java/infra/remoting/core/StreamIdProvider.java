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

import io.netty.util.collection.IntObjectMap;

/**
 * This API is not thread-safe and must be strictly used in serialized fashion
 */
final class StreamIdProvider {

  private static final int MASK = 0x7FFFFFFF;

  private long streamId;

  public StreamIdProvider(int streamId) {
    this.streamId = streamId;
  }

  public static StreamIdProvider forClient() {
    return new StreamIdProvider(-1);
  }

  public static StreamIdProvider forServer() {
    return new StreamIdProvider(0);
  }

  /**
   * This methods provides new stream id and ensures there is no intersections with already running
   * streams. This methods is not thread-safe.
   *
   * @param streamIds currently running streams store
   * @return next stream id
   */
  public int nextStreamId(IntObjectMap<?> streamIds) {
    int streamId;
    do {
      this.streamId += 2;
      streamId = (int) (this.streamId & MASK);
    }
    while (streamId == 0 || streamIds.containsKey(streamId));
    return streamId;
  }

  public boolean isBeforeOrCurrent(int streamId) {
    return this.streamId >= streamId && streamId > 0;
  }

}
