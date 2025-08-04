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

package infra.remoting.examples.tcp.lease.advanced.common;

import com.netflix.concurrency.limits.Limit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

import infra.remoting.frame.FrameType;
import infra.remoting.plugins.RequestInterceptor;
import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;

public class LimitBasedStatsCollector extends AtomicBoolean implements RequestInterceptor {

  final LeaseManager leaseManager;
  final Limit limitAlgorithm;

  final ConcurrentMap<Integer, Integer> inFlightMap = new ConcurrentHashMap<>();
  final ConcurrentMap<Integer, Long> timeMap = new ConcurrentHashMap<>();

  final LongSupplier clock = System::nanoTime;

  public LimitBasedStatsCollector(LeaseManager leaseManager, Limit limitAlgorithm) {
    this.leaseManager = leaseManager;
    this.limitAlgorithm = limitAlgorithm;
  }

  @Override
  public void onStart(int streamId, FrameType requestType, @Nullable ByteBuf metadata) {
    long startTime = clock.getAsLong();

    int currentInFlight = leaseManager.incrementInFlightAndGet();

    inFlightMap.put(streamId, currentInFlight);
    timeMap.put(streamId, startTime);
  }

  @Override
  public void onReject(
          Throwable rejectionReason, FrameType requestType, @Nullable ByteBuf metadata) { }

  @Override
  public void onTerminate(int streamId, FrameType requestType, @Nullable Throwable t) {
    leaseManager.decrementInFlight();

    Long startTime = timeMap.remove(streamId);
    Integer currentInflight = inFlightMap.remove(streamId);

    limitAlgorithm.onSample(startTime, clock.getAsLong() - startTime, currentInflight, t != null);
  }

  @Override
  public void onCancel(int streamId, FrameType requestType) {
    leaseManager.decrementInFlight();

    Long startTime = timeMap.remove(streamId);
    Integer currentInflight = inFlightMap.remove(streamId);

    limitAlgorithm.onSample(startTime, clock.getAsLong() - startTime, currentInflight, true);
  }

  @Override
  public boolean isDisposed() {
    return get();
  }

  @Override
  public void dispose() {
    if (!getAndSet(true)) {
      leaseManager.unregister();
    }
  }
}
