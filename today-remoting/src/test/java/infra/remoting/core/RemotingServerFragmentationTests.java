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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import infra.remoting.Closeable;
import infra.remoting.FrameAssert;
import infra.remoting.frame.FrameType;
import infra.remoting.test.util.TestClientTransport;
import infra.remoting.test.util.TestServerTransport;

public class RemotingServerFragmentationTests {

  @Test
  public void serverErrorsWithEnabledFragmentationOnInsufficientMtu() {
    Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> RemotingServer.create().fragment(2))
            .withMessage("The smallest allowed mtu size is 64 bytes, provided: 2");
  }

  @Test
  public void serverSucceedsWithEnabledFragmentationOnSufficientMtu() {
    TestServerTransport transport = new TestServerTransport();
    Closeable closeable = RemotingServer.create().fragment(100).bind(transport).block();
    closeable.dispose();
    transport.alloc().assertHasNoLeaks();
  }

  @Test
  public void serverSucceedsWithDisabledFragmentation() {
    TestServerTransport transport = new TestServerTransport();
    Closeable closeable = RemotingServer.create().bind(transport).block();
    closeable.dispose();
    transport.alloc().assertHasNoLeaks();
  }

  @Test
  public void clientErrorsWithEnabledFragmentationOnInsufficientMtu() {
    Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> ChannelConnector.create().fragment(2))
            .withMessage("The smallest allowed mtu size is 64 bytes, provided: 2");
  }

  @Test
  public void clientSucceedsWithEnabledFragmentationOnSufficientMtu() {
    TestClientTransport transport = new TestClientTransport();
    ChannelConnector.create().fragment(100).connect(transport).block();
    FrameAssert.assertThat(transport.testConnection().pollFrame())
            .typeOf(FrameType.SETUP)
            .hasNoLeaks();
    transport.testConnection().dispose();
    transport.alloc().assertHasNoLeaks();
  }

  @Test
  public void clientSucceedsWithDisabledFragmentation() {
    TestClientTransport transport = new TestClientTransport();
    ChannelConnector.connectWith(transport).block();
    FrameAssert.assertThat(transport.testConnection().pollFrame())
            .typeOf(FrameType.SETUP)
            .hasNoLeaks();
    transport.testConnection().dispose();
    transport.alloc().assertHasNoLeaks();
  }
}
