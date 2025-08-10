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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

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
    logger.info("Received new leased Connection[" + connectionId + "]");

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
