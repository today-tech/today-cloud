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

import org.HdrHistogram.Recorder;

import java.time.Duration;

import infra.remoting.Channel;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.test.PingClient;
import infra.remoting.test.PingHandler;
import reactor.core.publisher.Mono;

public final class LocalPingPong {

  public static void main(String... args) {
    RemotingServer.create(new PingHandler())
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .bind(LocalServerTransport.create("test-local-server"))
            .block();

    Mono<Channel> client =
            ChannelConnector.create()
                    .payloadDecoder(PayloadDecoder.ZERO_COPY)
                    .connect(LocalClientTransport.create("test-local-server"));

    PingClient pingClient = new PingClient(client);

    Recorder recorder = pingClient.startTracker(Duration.ofSeconds(1));

    int count = 1_000_000_000;

    pingClient
            .requestResponsePingPong(count, recorder)
            .doOnTerminate(() -> System.out.println("Sent " + count + " messages."))
            .blockLast();
  }
}
