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

package infra.remoting.transport.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.time.Duration;
import java.util.UUID;

import infra.remoting.test.TransportPair;
import infra.remoting.test.TransportTest;

final class LocalResumableWithFragmentationTransportTests implements TransportTest {

  private TransportPair transportPair;

  @BeforeEach
  void createTestPair(TestInfo testInfo) {
    transportPair =
            new TransportPair<>(
                    () ->
                            "LocalResumableWithFragmentationTransportTest-"
                                    + testInfo.getDisplayName()
                                    + "-"
                                    + UUID.randomUUID(),
                    (address, server, allocator) -> LocalClientTransport.create(address, allocator),
                    (address, allocator) -> LocalServerTransport.create(address),
                    true,
                    true);
  }

  @Override
  public Duration getTimeout() {
    return Duration.ofMinutes(1);
  }

  @Override
  public TransportPair getTransportPair() {
    return transportPair;
  }
}
