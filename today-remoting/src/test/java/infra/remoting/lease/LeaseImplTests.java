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

package infra.remoting.lease;

public class LeaseImplTests {
  //
  //  @Test
  //  public void emptyLeaseNoAvailability() {
  //    LeaseImpl empty = LeaseImpl.empty();
  //    Assertions.assertTrue(empty.isEmpty());
  //    Assertions.assertFalse(empty.isValid());
  //    Assertions.assertEquals(0.0, empty.availability(), 1e-5);
  //  }
  //
  //  @Test
  //  public void emptyLeaseUseNoAvailability() {
  //    LeaseImpl empty = LeaseImpl.empty();
  //    boolean success = empty.use();
  //    assertFalse(success);
  //    Assertions.assertEquals(0.0, empty.availability(), 1e-5);
  //  }
  //
  //  @Test
  //  public void leaseAvailability() {
  //    LeaseImpl lease = LeaseImpl.create(2, 100, Unpooled.EMPTY_BUFFER);
  //    Assertions.assertEquals(1.0, lease.availability(), 1e-5);
  //  }
  //
  //  @Test
  //  public void leaseUseDecreasesAvailability() {
  //    LeaseImpl lease = LeaseImpl.create(30_000, 2, Unpooled.EMPTY_BUFFER);
  //    boolean success = lease.use();
  //    Assertions.assertTrue(success);
  //    Assertions.assertEquals(0.5, lease.availability(), 1e-5);
  //    Assertions.assertTrue(lease.isValid());
  //    success = lease.use();
  //    Assertions.assertTrue(success);
  //    Assertions.assertEquals(0.0, lease.availability(), 1e-5);
  //    Assertions.assertFalse(lease.isValid());
  //    Assertions.assertEquals(0, lease.getAllowedRequests());
  //    success = lease.use();
  //    Assertions.assertFalse(success);
  //  }
  //
  //  @Test
  //  public void leaseTimeout() {
  //    int numberOfRequests = 1;
  //    LeaseImpl lease = LeaseImpl.create(1, numberOfRequests, Unpooled.EMPTY_BUFFER);
  //    Mono.delay(Duration.ofMillis(100)).block();
  //    boolean success = lease.use();
  //    Assertions.assertFalse(success);
  //    Assertions.assertTrue(lease.isExpired());
  //    Assertions.assertEquals(numberOfRequests, lease.getAllowedRequests());
  //    Assertions.assertFalse(lease.isValid());
  //  }
  //
  //  @Test
  //  public void useLeaseChangesAllowedRequests() {
  //    int numberOfRequests = 2;
  //    LeaseImpl lease = LeaseImpl.create(30_000, numberOfRequests, Unpooled.EMPTY_BUFFER);
  //    lease.use();
  //    assertEquals(numberOfRequests - 1, lease.getAllowedRequests());
  //  }
}
