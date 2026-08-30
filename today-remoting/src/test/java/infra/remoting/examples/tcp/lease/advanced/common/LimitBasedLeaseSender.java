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

import java.time.Duration;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.lease.Lease;
import infra.remoting.lease.TrackingLeaseSender;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

public class LimitBasedLeaseSender extends LimitBasedStatsCollector implements TrackingLeaseSender {

  static final Logger logger = LoggerFactory.getLogger(LimitBasedLeaseSender.class);

  final String connectionId;
  final Sinks.Many<Lease> sink =
          Sinks.many().unicast().onBackpressureBuffer(Queues.<Lease>one().get());

  public LimitBasedLeaseSender(
          String connectionId, LeaseManager leaseManager, Limit limitAlgorithm) {
    super(leaseManager, limitAlgorithm);
    this.connectionId = connectionId;
  }

  @Override
  public Flux<Lease> send() {
    logger.info("Received new leased Connection[{}]", connectionId);

    leaseManager.register(this);

    return sink.asFlux();
  }

  public void sendLease(int ttl, int amount) {
    final Lease nextLease = Lease.create(Duration.ofMillis(ttl), amount);
    final Sinks.EmitResult result = sink.tryEmitNext(nextLease);

    if (result.isFailure()) {
      logger.warn(
              "Connection["
                      + connectionId
                      + "]. Issued Lease: ["
                      + nextLease
                      + "] was not sent due to "
                      + result);
    }
    else {
      if (logger.isDebugEnabled()) {
        logger.debug("To Connection[" + connectionId + "]: Issued Lease: [" + nextLease + "]");
      }
    }
  }
}
