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
