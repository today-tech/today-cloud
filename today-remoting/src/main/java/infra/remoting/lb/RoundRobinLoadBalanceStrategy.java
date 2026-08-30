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
package infra.remoting.lb;

import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import infra.remoting.Channel;

/**
 * Simple {@link LoadBalanceStrategy} that selects the {@code Channel} to use in round-robin order.
 */
public class RoundRobinLoadBalanceStrategy implements LoadBalanceStrategy {

  volatile int nextIndex;

  private static final AtomicIntegerFieldUpdater<RoundRobinLoadBalanceStrategy> NEXT_INDEX =
          AtomicIntegerFieldUpdater.newUpdater(RoundRobinLoadBalanceStrategy.class, "nextIndex");

  @Override
  public Channel select(List<Channel> channels) {
    int length = channels.size();
    int indexToUse = Math.abs(NEXT_INDEX.getAndIncrement(this) % length);
    return channels.get(indexToUse);
  }

}
