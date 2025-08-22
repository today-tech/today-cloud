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

import infra.lang.Nullable;
import infra.remoting.Availability;
import infra.remoting.Connection;
import infra.remoting.frame.LeaseFrameCodec;
import infra.remoting.lease.Lease;
import infra.remoting.lease.LeaseSender;
import infra.remoting.lease.MissingLeaseException;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;

final class ResponderLeaseTracker extends BaseSubscriber<Lease> implements Disposable, Availability {

  final String tag;
  final ByteBufAllocator allocator;
  final Connection connection;

  @Nullable
  volatile MutableLease currentLease;

  ResponderLeaseTracker(String tag, Connection connection, LeaseSender leaseSender) {
    this.tag = tag;
    this.connection = connection;
    this.allocator = connection.alloc();

    leaseSender.send().subscribe(this);
  }

  @Nullable
  public Throwable use() {
    final MutableLease lease = this.currentLease;
    final String tag = this.tag;

    if (lease == null) {
      return new MissingLeaseException(String.format("[%s] Lease was not issued yet", tag));
    }

    if (isExpired(lease)) {
      return new MissingLeaseException(String.format("[%s] Missing leases. Lease is expired", tag));
    }

    final int allowedRequests = lease.allowedRequests;
    final int remainingRequests = lease.remainingRequests;
    if (remainingRequests <= 0) {
      return new MissingLeaseException(String.format(
              "[%s] Missing leases. Issued [%s] request allowance is used", tag, allowedRequests));
    }

    lease.remainingRequests = remainingRequests - 1;
    return null;
  }

  @Override
  protected void hookOnNext(Lease lease) {
    final int allowedRequests = lease.numberOfRequests();
    final int ttl = lease.timeToLiveInMillis();
    final long expireAt = lease.expirationTime();

    this.currentLease = new MutableLease(allowedRequests, expireAt);
    this.connection.sendFrame(0, LeaseFrameCodec.encode(this.allocator, ttl, allowedRequests, lease.metadata()));
  }

  @Override
  public double availability() {
    final MutableLease lease = this.currentLease;

    if (lease == null || isExpired(lease)) {
      return 0;
    }

    return lease.remainingRequests / (double) lease.allowedRequests;
  }

  static boolean isExpired(MutableLease currentLease) {
    return System.currentTimeMillis() >= currentLease.expireAt;
  }

  static final class MutableLease {
    final int allowedRequests;
    final long expireAt;

    int remainingRequests;

    MutableLease(int allowedRequests, long expireAt) {
      this.allowedRequests = allowedRequests;
      this.expireAt = expireAt;

      this.remainingRequests = allowedRequests;
    }
  }
}
