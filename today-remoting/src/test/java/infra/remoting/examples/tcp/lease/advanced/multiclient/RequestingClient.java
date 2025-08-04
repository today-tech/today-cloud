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

package infra.remoting.examples.tcp.lease.advanced.multiclient;

import java.util.Objects;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.core.ChannelConnector;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.util.ByteBufPayload;
import reactor.core.publisher.Flux;

public class RequestingClient {
  private static final Logger logger = LoggerFactory.getLogger(RequestingClient.class);

  public static void main(String[] args) {

    Channel clientChannel =
            ChannelConnector.create()
                    .lease()
                    .connect(TcpClientTransport.create("localhost", 7000))
                    .block();

    Objects.requireNonNull(clientChannel);

    // generate stream of fnfs
    Flux.generate(
                    () -> 0L,
                    (state, sink) -> {
                      sink.next(state);
                      return state + 1;
                    })
            .concatMap(
                    tick -> {
                      logger.info("Requesting FireAndForget({})", tick);
                      return clientChannel.fireAndForget(ByteBufPayload.create("" + tick));
                    })
            .blockLast();

    clientChannel.onClose().block();
  }
}
