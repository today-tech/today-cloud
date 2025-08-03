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
 * Contract for performing protocol requests.
 *
 * <p>{@link RemotingClient} differs from {@link Channel} in a number of ways:
 *
 * <ul>
 *   <li>{@code Channel} represents a "live" connection that is transient and needs to be obtained
 *       typically from a {@code Mono<Channel>} source via {@code flatMap} or block. By contrast,
 *       {@code RemotingClient} is a higher level layer that contains such a {@link #source() source}
 *       of connections and transparently obtains and re-obtains a shared connection as needed when
 *       requests are made concurrently. That means an {@code RemotingClient} can simply be created
 *       once, even before a connection is established, and shared as a singleton across multiple
 *       places as you would with any other client.
 *   <li>For request input {@code Channel} accepts an instance of {@code Payload} and does not allow
 *       more than one subscription per request because there is no way to safely re-use that input.
 *       By contrast {@code RemotingClient} accepts {@code Publisher<Payload>} and allow
 *       re-subscribing which repeats the request.
 *   <li>{@code Channel} can be used for sending and it can also be implemented for receiving. By
 *       contrast {@code RemotingClient} is used only for sending, typically from the client side
 *       which allows obtaining and re-obtaining connections from a source as needed. However it can
 *       also be used from the server side by {@link #from(Channel) wrapping} the "live" {@code
 *       Channel} for a given connection.
 * </ul>
 *
 * <p>The example below shows how to create an {@code RemotingClient}:
 *
 * <pre>{@code
 * Mono<Channel> source = ChannelConnector.create()
 *      .connect(TcpClientTransport.create("localhost", 7000));
 *
 * RemotingClient client = RemotingClient.from(source);
 * }</pre>
 *
 * <p>The below configures retry logic to use when a shared {@code Channel} connection is obtained:
 *
 * <pre>{@code
 * Mono<Channel> source =
 *         ChannelConnector.create()
 *                 .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
 *                 .connect(TcpClientTransport.create("localhost", 7000));
 * RemotingClient client = RemotingClient.from(source);
 * }</pre>
 *
 * @see LoadBalanceRemotingClient
 */
public interface RemotingClient extends Closeable {

  /**
   * Connect to the remote endpoint, if not yet connected. This method is a shortcut for
   * {@code RemotingClient#source().subscribe()}.
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
   * made, from the given {@code Mono<Channel>} source.
   *
   * @param source the source for connections, typically prepared via {@link ChannelConnector}.
   * @return the created client instance
   */
  static RemotingClient from(Mono<Channel> source) {
    return new DefaultRemotingClient(source);
  }

  /**
   * Adapt the given {@link Channel} to use as {@link RemotingClient}. This is useful to wrap the
   * sending {@code Channel} in a server.
   *
   * <p><strong>Note:</strong> unlike an {@code RemotingClient} created via {@link
   * RemotingClient#from(Mono)}, the instance returned from this factory method can only perform
   * requests for as long as the given {@code Channel} remains "live".
   *
   * @param channel the {@code Channel} to perform requests with
   * @return the created client instance
   */
  static RemotingClient from(Channel channel) {
    return new RemotingClientAdapter(channel);
  }
}
