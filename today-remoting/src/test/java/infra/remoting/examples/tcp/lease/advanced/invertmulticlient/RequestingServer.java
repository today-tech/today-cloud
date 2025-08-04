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
