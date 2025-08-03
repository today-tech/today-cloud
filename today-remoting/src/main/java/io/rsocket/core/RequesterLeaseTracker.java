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

package io.rsocket.core;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;

import io.netty.buffer.ByteBuf;
import io.rsocket.Availability;
import io.rsocket.frame.LeaseFrameCodec;
import io.rsocket.lease.Lease;
import io.rsocket.lease.MissingLeaseException;

final class RequesterLeaseTracker implements Availability {

  final String tag;

  final int maximumAllowedAwaitingPermitHandlersNumber;

  final ArrayDeque<LeasePermitHandler> awaitingPermitHandlersQueue;

  Lease currentLease = null;

  int availableRequests;

  boolean isDisposed;

  Throwable t;

  RequesterLeaseTracker(String tag, int maximumAllowedAwaitingPermitHandlersNumber) {
    this.tag = tag;
    this.maximumAllowedAwaitingPermitHandlersNumber = maximumAllowedAwaitingPermitHandlersNumber;
    this.awaitingPermitHandlersQueue = new ArrayDeque<>();
  }

  synchronized void issue(LeasePermitHandler leasePermitHandler) {
    if (this.isDisposed) {
      leasePermitHandler.handlePermitError(this.t);
      return;
    }

    final int availableRequests = this.availableRequests;
    final Lease l = this.currentLease;
    final boolean leaseReceived = l != null;
    final boolean isExpired = leaseReceived && isExpired(l);

    if (leaseReceived && availableRequests > 0 && !isExpired) {
      if (leasePermitHandler.handlePermit()) {
        this.availableRequests = availableRequests - 1;
      }
    }
    else {
      final Queue<LeasePermitHandler> queue = this.awaitingPermitHandlersQueue;
      if (this.maximumAllowedAwaitingPermitHandlersNumber > queue.size()) {
        queue.offer(leasePermitHandler);
      }
      else {
        final String tag = this.tag;
        final String message;
        if (!leaseReceived) {
          message = String.format("[%s] Lease was not received yet", tag);
        }
        else if (isExpired) {
          message = String.format("[%s] Missing leases. Lease is expired", tag);
        }
        else {
          message = String.format("[%s] Missing leases. Issued [%s] request allowance is used", tag, availableRequests);
        }

        final Throwable t = new MissingLeaseException(message);
        leasePermitHandler.handlePermitError(t);
      }
    }
  }

  void handleLeaseFrame(ByteBuf leaseFrame) {
    final int numberOfRequests = LeaseFrameCodec.numRequests(leaseFrame);
    final int timeToLiveMillis = LeaseFrameCodec.ttl(leaseFrame);
    final ByteBuf metadata = LeaseFrameCodec.metadata(leaseFrame);

    synchronized(this) {
      final Lease lease = Lease.create(Duration.ofMillis(timeToLiveMillis), numberOfRequests, metadata);
      final Queue<LeasePermitHandler> queue = this.awaitingPermitHandlersQueue;

      int availableRequests = lease.numberOfRequests();

      this.currentLease = lease;
      if (!queue.isEmpty()) {
        do {
          final LeasePermitHandler handler = queue.poll();
          if (handler.handlePermit()) {
            availableRequests--;
          }
        }
        while (availableRequests > 0 && !queue.isEmpty());
      }

      this.availableRequests = availableRequests;
    }
  }

  public synchronized void dispose(Throwable t) {
    this.isDisposed = true;
    this.t = t;

    final Queue<LeasePermitHandler> queue = this.awaitingPermitHandlersQueue;
    final int size = queue.size();

    for (int i = 0; i < size; i++) {
      final LeasePermitHandler leasePermitHandler = queue.poll();

      //noinspection ConstantConditions
      leasePermitHandler.handlePermitError(t);
    }
  }

  @Override
  public synchronized double availability() {
    final Lease lease = this.currentLease;
    return lease != null ? this.availableRequests / (double) lease.numberOfRequests() : 0.0d;
  }

  static boolean isExpired(Lease currentLease) {
    return System.currentTimeMillis() >= currentLease.expirationTime();
  }

}
