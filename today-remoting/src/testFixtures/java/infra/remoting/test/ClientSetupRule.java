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

package infra.remoting.test;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.remoting.Channel;
import infra.remoting.Closeable;
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
                    RemotingServer.create((setup, channel) -> Mono.just(new TestChannel(data, metadata)))
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

  public Channel getChannel() {
    return client;
  }

  public String expectedPayloadData() {
    return data;
  }

  public String expectedPayloadMetadata() {
    return metadata;
  }
}
