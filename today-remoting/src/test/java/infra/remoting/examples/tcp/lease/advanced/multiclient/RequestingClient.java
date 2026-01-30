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
