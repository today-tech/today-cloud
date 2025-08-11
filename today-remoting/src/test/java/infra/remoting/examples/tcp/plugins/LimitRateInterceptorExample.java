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

package infra.remoting.examples.tcp.plugins;

import org.reactivestreams.Publisher;

import java.time.Duration;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.plugins.RateLimitDecorator;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;

public class LimitRateInterceptorExample {

  private static final Logger logger = LoggerFactory.getLogger(LimitRateInterceptorExample.class);

  public static void main(String[] args) {
    RemotingServer.create(
                    ChannelAcceptor.with(
                            new Channel() {
                              @Override
                              public Flux<Payload> requestStream(Payload payload) {
                                return Flux.interval(Duration.ofMillis(100))
                                        .doOnRequest(
                                                e -> logger.debug("Server publisher receives request for " + e))
                                        .map(aLong -> DefaultPayload.create("Interval: " + aLong));
                              }

                              @Override
                              public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                                return Flux.from(payloads)
                                        .doOnRequest(
                                                e -> logger.debug("Server publisher receives request for " + e));
                              }
                            }))
            .interceptors(registry -> registry.forResponder(RateLimitDecorator.forResponder(64)))
            .bindNow(TcpServerTransport.create("localhost", 7000));

    Channel channel =
            ChannelConnector.create()
                    .interceptors(registry -> registry.forRequester(RateLimitDecorator.forRequester(64)))
                    .connect(TcpClientTransport.create("localhost", 7000))
                    .block();

    logger.debug(
            "\n\nStart of requestStream interaction\n" + "----------------------------------\n");

    channel
            .requestStream(DefaultPayload.create("Hello"))
            .doOnRequest(e -> logger.debug("Client sends requestN(" + e + ")"))
            .map(Payload::getDataUtf8)
            .doOnNext(logger::debug)
            .take(10)
            .then()
            .block();

    logger.debug(
            "\n\nStart of requestChannel interaction\n" + "-----------------------------------\n");

    channel
            .requestChannel(
                    Flux.<Payload, Long>generate(
                                    () -> 1L,
                                    (s, sink) -> {
                                      sink.next(DefaultPayload.create("Next " + s));
                                      return ++s;
                                    })
                            .doOnRequest(e -> logger.debug("Client publisher receives request for " + e)))
            .doOnRequest(e -> logger.debug("Client sends requestN(" + e + ")"))
            .map(Payload::getDataUtf8)
            .doOnNext(logger::debug)
            .take(10)
            .then()
            .doFinally(signalType -> channel.dispose())
            .then()
            .block();
  }
}
