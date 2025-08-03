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

package io.rsocket.lease;

import java.time.Duration;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** A contract for RSocket lease, which is sent by a request acceptor and is time bound. */
public final class Lease {

  public static Lease create(
          Duration timeToLive, int numberOfRequests, @Nullable ByteBuf metadata) {
    return new Lease(timeToLive, numberOfRequests, metadata);
  }

  public static Lease create(Duration timeToLive, int numberOfRequests) {
    return create(timeToLive, numberOfRequests, Unpooled.EMPTY_BUFFER);
  }

  public static Lease unbounded() {
    return unbounded(null);
  }

  public static Lease unbounded(@Nullable ByteBuf metadata) {
    return create(Duration.ofMillis(Integer.MAX_VALUE), Integer.MAX_VALUE, metadata);
  }

  public static Lease empty() {
    return create(Duration.ZERO, 0);
  }

  final int timeToLiveMillis;
  final int numberOfRequests;
  final ByteBuf metadata;
  final long expirationTime;

  Lease(Duration timeToLive, int numberOfRequests, @Nullable ByteBuf metadata) {
    this.numberOfRequests = numberOfRequests;
    this.timeToLiveMillis = (int) Math.min(timeToLive.toMillis(), Integer.MAX_VALUE);
    this.metadata = metadata == null ? Unpooled.EMPTY_BUFFER : metadata;
    this.expirationTime =
            timeToLive.isZero() ? 0 : System.currentTimeMillis() + timeToLive.toMillis();
  }

  /**
   * Number of requests allowed by this lease.
   *
   * @return The number of requests allowed by this lease.
   */
  public int numberOfRequests() {
    return numberOfRequests;
  }

  /**
   * Time to live for the given lease
   *
   * @return relative duration in milliseconds
   */
  public int timeToLiveInMillis() {
    return this.timeToLiveMillis;
  }

  /**
   * Absolute time since epoch at which this lease will expire.
   *
   * @return Absolute time since epoch at which this lease will expire.
   */
  public long expirationTime() {
    return expirationTime;
  }

  /**
   * Metadata for the lease.
   *
   * @return Metadata for the lease.
   */
  @Nullable
  public ByteBuf metadata() {
    return metadata;
  }

  @Override
  public String toString() {
    return "Lease{"
            + "timeToLiveMillis="
            + timeToLiveMillis
            + ", numberOfRequests="
            + numberOfRequests
            + ", expirationTime="
            + expirationTime
            + '}';
  }
}
