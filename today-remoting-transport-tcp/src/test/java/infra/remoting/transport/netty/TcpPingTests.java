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

package infra.remoting.transport.netty;

import org.HdrHistogram.Recorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.remoting.Channel;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.Resume;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.test.PerfTest;
import infra.remoting.test.PingClient;
import infra.remoting.transport.netty.client.TcpClientTransport;
import reactor.core.publisher.Mono;

@PerfTest
class TcpPingTests {
  private static final int INTERACTIONS_COUNT = 1_000_000_000;
  private static final int port = Integer.valueOf(System.getProperty("REMOTING_TEST_PORT", "7878"));

  @BeforeEach
  void setUp() {
    System.out.println("Starting ping-pong test (TCP transport)");
    System.out.println("port: " + port);
  }

  @Test
  void requestResponseTest() {
    PingClient pingClient = newPingClient();
    Recorder recorder = pingClient.startTracker(Duration.ofSeconds(1));

    pingClient
            .requestResponsePingPong(INTERACTIONS_COUNT, recorder)
            .doOnTerminate(() -> System.out.println("Sent " + INTERACTIONS_COUNT + " messages."))
            .blockLast();
  }

  @Test
  void requestStreamTest() {
    PingClient pingClient = newPingClient();
    Recorder recorder = pingClient.startTracker(Duration.ofSeconds(1));

    pingClient
            .requestStreamPingPong(INTERACTIONS_COUNT, recorder)
            .doOnTerminate(() -> System.out.println("Sent " + INTERACTIONS_COUNT + " messages."))
            .blockLast();
  }

  @Test
  void requestStreamResumableTest() {
    PingClient pingClient = newResumablePingClient();
    Recorder recorder = pingClient.startTracker(Duration.ofSeconds(1));

    pingClient
            .requestStreamPingPong(INTERACTIONS_COUNT, recorder)
            .doOnTerminate(() -> System.out.println("Sent " + INTERACTIONS_COUNT + " messages."))
            .blockLast();
  }

  private static PingClient newPingClient() {
    return newPingClient(false);
  }

  private static PingClient newResumablePingClient() {
    return newPingClient(true);
  }

  private static PingClient newPingClient(boolean isResumable) {
    ChannelConnector connector = ChannelConnector.create();
    if (isResumable) {
      connector.resume(new Resume());
    }
    Mono<Channel> channel =
            connector
                    .payloadDecoder(PayloadDecoder.ZERO_COPY)
                    .keepAlive(Duration.ofMinutes(1), Duration.ofMinutes(30))
                    .connect(TcpClientTransport.create(port));

    return new PingClient(channel);
  }
}
