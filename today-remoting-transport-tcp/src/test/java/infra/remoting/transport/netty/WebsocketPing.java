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

import java.time.Duration;

import infra.remoting.Channel;
import infra.remoting.core.ChannelConnector;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.test.PingClient;
import infra.remoting.transport.netty.client.WebsocketClientTransport;
import reactor.core.publisher.Mono;

public final class WebsocketPing {

  public static void main(String... args) {
    Mono<Channel> client =
            ChannelConnector.create()
                    .payloadDecoder(PayloadDecoder.ZERO_COPY)
                    .connect(WebsocketClientTransport.create(7878));

    PingClient pingClient = new PingClient(client);

    Recorder recorder = pingClient.startTracker(Duration.ofSeconds(1));

    int count = 1_000_000_000;

    pingClient
            .requestResponsePingPong(count, recorder)
            .doOnTerminate(() -> System.out.println("Sent " + count + " messages."))
            .blockLast();
  }
}
