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
package io.rsocket.lb;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.rsocket.frame.FrameType;
import io.rsocket.plugins.RequestInterceptor;

/**
 * {@link RequestInterceptor} that hooks into request lifecycle and calls methods of the parent
 * class to manage tracking state and expose {@link WeightedStats}.
 *
 * <p>This interceptor the default mechanism for gathering stats when {@link
 * WeightedLoadBalanceStrategy} is used with {@link LoadBalanceRemotingClient}.
 *
 * @see LoadBalanceRemotingClient
 * @see WeightedLoadBalanceStrategy
 */
public class WeightedStatsRequestInterceptor extends BaseWeightedStats implements RequestInterceptor {

  final Int2LongHashMap requestsStartTime = new Int2LongHashMap(-1);

  public WeightedStatsRequestInterceptor() {
    super();
  }

  @Override
  public final void onStart(int streamId, FrameType requestType, @Nullable ByteBuf metadata) {
    switch (requestType) {
      case REQUEST_FNF:
      case REQUEST_RESPONSE:
        final long startTime = startRequest();
        final Int2LongHashMap requestsStartTime = this.requestsStartTime;
        synchronized(requestsStartTime) {
          requestsStartTime.put(streamId, startTime);
        }
        break;
      case REQUEST_STREAM:
      case REQUEST_CHANNEL:
        this.startStream();
    }
  }

  @Override
  public final void onTerminate(int streamId, FrameType requestType, @Nullable Throwable t) {
    switch (requestType) {
      case REQUEST_FNF:
      case REQUEST_RESPONSE:
        long startTime;
        final Int2LongHashMap requestsStartTime = this.requestsStartTime;
        synchronized(requestsStartTime) {
          startTime = requestsStartTime.remove(streamId);
        }
        long endTime = stopRequest(startTime);
        if (t == null) {
          record(endTime - startTime);
        }
        break;
      case REQUEST_STREAM:
      case REQUEST_CHANNEL:
        stopStream();
        break;
    }

    if (t != null) {
      updateAvailability(0.0d);
    }
    else {
      updateAvailability(1.0d);
    }
  }

  @Override
  public final void onCancel(int streamId, FrameType requestType) {
    switch (requestType) {
      case REQUEST_FNF:
      case REQUEST_RESPONSE:
        long startTime;
        final Int2LongHashMap requestsStartTime = this.requestsStartTime;
        synchronized(requestsStartTime) {
          startTime = requestsStartTime.remove(streamId);
        }
        stopRequest(startTime);
        break;
      case REQUEST_STREAM:
      case REQUEST_CHANNEL:
        stopStream();
        break;
    }
  }

  @Override
  public final void onReject(Throwable rejectionReason, FrameType requestType, @Nullable ByteBuf metadata) {
  }

  @Override
  public void dispose() {
  }

}
