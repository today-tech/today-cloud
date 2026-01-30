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

package infra.remoting.core;

import org.jspecify.annotations.Nullable;

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
