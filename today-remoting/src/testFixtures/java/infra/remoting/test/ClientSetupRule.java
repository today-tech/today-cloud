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

package infra.remoting.test;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.remoting.Closeable;
import infra.remoting.Channel;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.ClientTransport;
import infra.remoting.transport.ServerTransport;
import reactor.core.publisher.Mono;

public class ClientSetupRule<T, S extends Closeable> {
  private static final String data = "hello world";
  private static final String metadata = "metadata";

  private Supplier<T> addressSupplier;
  private BiFunction<T, S, Channel> clientConnector;
  private Function<T, S> serverInit;

  private Channel client;
  private S server;

  public ClientSetupRule(
          Supplier<T> addressSupplier,
          BiFunction<T, S, ClientTransport> clientTransportSupplier,
          Function<T, ServerTransport<S>> serverTransportSupplier) {
    this.addressSupplier = addressSupplier;

    this.serverInit =
            address ->
                    RemotingServer.create((setup, rsocket) -> Mono.just(new TestChannel(data, metadata)))
                            .bind(serverTransportSupplier.apply(address))
                            .block();

    this.clientConnector =
            (address, server) ->
                    ChannelConnector.connectWith(clientTransportSupplier.apply(address, server))
                            .doOnError(Throwable::printStackTrace)
                            .block();
  }

  public void init() {
    T address = addressSupplier.get();
    S server = serverInit.apply(address);
    client = clientConnector.apply(address, server);
  }

  public void tearDown() {
    server.dispose();
  }

  public Channel getRSocket() {
    return client;
  }

  public String expectedPayloadData() {
    return data;
  }

  public String expectedPayloadMetadata() {
    return metadata;
  }
}
