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
