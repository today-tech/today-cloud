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
