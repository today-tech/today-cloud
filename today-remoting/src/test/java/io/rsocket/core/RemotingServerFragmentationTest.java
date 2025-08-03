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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.rsocket.Closeable;
import io.rsocket.FrameAssert;
import io.rsocket.frame.FrameType;
import io.rsocket.test.util.TestClientTransport;
import io.rsocket.test.util.TestServerTransport;

public class RemotingServerFragmentationTest {

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
