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

package infra.remoting.examples.tcp.lease.advanced.invertmulticlient;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RequestingServer {

  private static final Logger logger = LoggerFactory.getLogger(RequestingServer.class);

  public static void main(String[] args) {
    PriorityBlockingQueue<Channel> channels =
            new PriorityBlockingQueue<>(
                    16, Comparator.comparingDouble(Channel::availability).reversed());

    CloseableChannel server =
            RemotingServer.create(
                            (setup, channel) -> {
                              logger.info("Received new connection");
                              return Mono.<Channel>just(new Channel() { })
                                      .doAfterTerminate(() -> channels.put(channel));
                            })
                    .lease(spec -> spec.maxPendingRequests(Integer.MAX_VALUE))
                    .bindNow(TcpServerTransport.create("localhost", 7000));

    logger.info("Server started on port {}", server.address().getPort());

    // generate stream of fnfs
    Flux.generate(
                    () -> 0L,
                    (state, sink) -> {
                      sink.next(state);
                      return state + 1;
                    })
            .flatMap(
                    tick -> {
                      logger.info("Requesting FireAndForget({})", tick);

                      return Mono.fromCallable(
                                      () -> {
                                        Channel channel = channels.take();
                                        channels.offer(channel);
                                        return channel;
                                      })
                              .flatMap(
                                      clientChannel ->
                                              clientChannel.fireAndForget(ByteBufPayload.create("" + tick)))
                              .retry();
                    })
            .blockLast();

    server.onClose().block();
  }
}
