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
package infra.remoting.core;

import org.reactivestreams.Publisher;

import infra.remoting.Channel;
import infra.remoting.Closeable;
import infra.remoting.Payload;
import infra.remoting.lb.LoadBalanceRemotingClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contract for performing RSocket requests.
 *
 * <p>{@link RemotingClient} differs from {@link Channel} in a number of ways:
 *
 * <ul>
 *   <li>{@code RSocket} represents a "live" connection that is transient and needs to be obtained
 *       typically from a {@code Mono<RSocket>} source via {@code flatMap} or block. By contrast,
 *       {@code RSocketClient} is a higher level layer that contains such a {@link #source() source}
 *       of connections and transparently obtains and re-obtains a shared connection as needed when
 *       requests are made concurrently. That means an {@code RSocketClient} can simply be created
 *       once, even before a connection is established, and shared as a singleton across multiple
 *       places as you would with any other client.
 *   <li>For request input {@code RSocket} accepts an instance of {@code Payload} and does not allow
 *       more than one subscription per request because there is no way to safely re-use that input.
 *       By contrast {@code RSocketClient} accepts {@code Publisher<Payload>} and allow
 *       re-subscribing which repeats the request.
 *   <li>{@code RSocket} can be used for sending and it can also be implemented for receiving. By
 *       contrast {@code RSocketClient} is used only for sending, typically from the client side
 *       which allows obtaining and re-obtaining connections from a source as needed. However it can
 *       also be used from the server side by {@link #from(Channel) wrapping} the "live" {@code
 *       RSocket} for a given connection.
 * </ul>
 *
 * <p>The example below shows how to create an {@code RSocketClient}:
 *
 * <pre class="code">{@code
 * Mono<RSocket> source =
 *         RSocketConnector.create()
 *                 .metadataMimeType("message/x.rsocket.composite-metadata.v0")
 *                 .dataMimeType("application/cbor")
 *                 .connect(TcpClientTransport.create("localhost", 7000));
 *
 * RSocketClient client = RSocketClient.from(source);
 * }</pre>
 *
 * <p>The below configures retry logic to use when a shared {@code RSocket} connection is obtained:
 *
 * <pre class="code">{@code
 * Mono<RSocket> source =
 *         RSocketConnector.create()
 *                 .metadataMimeType("message/x.rsocket.composite-metadata.v0")
 *                 .dataMimeType("application/cbor")
 *                 .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
 *                 .connect(TcpClientTransport.create("localhost", 7000));
 *
 * RSocketClient client = RSocketClient.from(source);
 * }</pre>
 *
 * @see LoadBalanceRemotingClient
 */
public interface RemotingClient extends Closeable {

  /**
   * Connect to the remote rsocket endpoint, if not yet connected. This method is a shortcut for
   * {@code RSocketClient#source().subscribe()}.
   *
   * @return {@code true} if an attempt to connect was triggered or if already connected, or {@code
   * false} if the client is terminated.
   */
  default boolean connect() {
    throw new UnsupportedOperationException();
  }

  default Mono<Void> onClose() {
    return Mono.error(new UnsupportedOperationException());
  }

  /**
   * Return the underlying source used to obtain a shared {@link Channel} connection.
   */
  Mono<Channel> source();

  /**
   * Perform a Fire-and-Forget interaction via {@link Channel#fireAndForget(Payload)}. Allows
   * multiple subscriptions and performs a request per subscriber.
   */
  Mono<Void> fireAndForget(Mono<Payload> payloadMono);

  /**
   * Perform a Request-Response interaction via {@link Channel#requestResponse(Payload)}. Allows
   * multiple subscriptions and performs a request per subscriber.
   */
  Mono<Payload> requestResponse(Mono<Payload> payloadMono);

  /**
   * Perform a Request-Stream interaction via {@link Channel#requestStream(Payload)}. Allows
   * multiple subscriptions and performs a request per subscriber.
   */
  Flux<Payload> requestStream(Mono<Payload> payloadMono);

  /**
   * Perform a Request-Channel interaction via {@link Channel#requestChannel(Publisher)}. Allows
   * multiple subscriptions and performs a request per subscriber.
   */
  Flux<Payload> requestChannel(Publisher<Payload> payloads);

  /**
   * Perform a Metadata Push via {@link Channel#metadataPush(Payload)}. Allows multiple
   * subscriptions and performs a request per subscriber.
   */
  Mono<Void> metadataPush(Mono<Payload> payloadMono);

  /**
   * Create an {@link RemotingClient} that obtains shared connections as needed, when requests are
   * made, from the given {@code Mono<RSocket>} source.
   *
   * @param source the source for connections, typically prepared via {@link ChannelConnector}.
   * @return the created client instance
   */
  static RemotingClient from(Mono<Channel> source) {
    return new DefaultRemotingClient(source);
  }

  /**
   * Adapt the given {@link Channel} to use as {@link RemotingClient}. This is useful to wrap the
   * sending {@code RSocket} in a server.
   *
   * <p><strong>Note:</strong> unlike an {@code RSocketClient} created via {@link
   * RemotingClient#from(Mono)}, the instance returned from this factory method can only perform
   * requests for as long as the given {@code RSocket} remains "live".
   *
   * @param rsocket the {@code RSocket} to perform requests with
   * @return the created client instance
   */
  static RemotingClient from(Channel rsocket) {
    return new RemotingClientAdapter(rsocket);
  }
}
