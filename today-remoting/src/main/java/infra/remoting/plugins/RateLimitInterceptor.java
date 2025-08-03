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

package infra.remoting.plugins;

import org.reactivestreams.Publisher;

import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.util.ChannelDecorator;
import reactor.core.publisher.Flux;

/**
 * Interceptor that adds {@link Flux#limitRate(int, int)} to publishers of outbound streams that
 * breaks down or aggregates demand values from the remote end (i.e. {@code REQUEST_N} frames) into
 * batches of a uniform size. For example the remote may request {@code Long.MAXVALUE} or it may
 * start requesting one at a time, in both cases with the limit set to 64, the publisher will see a
 * demand of 64 to start and subsequent batches of 48, i.e. continuing to prefetch and refill an
 * internal queue when it falls to 75% full. The high and low tide marks are configurable.
 *
 * <p>See static factory methods to create an instance for a requester or for a responder.
 *
 * <p><strong>Note:</strong> keep in mind that the {@code limitRate} operator always uses requests
 * the same request values, even if the remote requests less than the limit. For example given a
 * limit of 64, if the remote requests 4, 64 will be prefetched of which 4 will be sent and 60 will
 * be cached.
 *
 * @since 1.0
 */
public class RateLimitInterceptor implements ChannelInterceptor {

  private final int highTide;

  private final int lowTide;

  private final boolean requesterProxy;

  private RateLimitInterceptor(int highTide, int lowTide, boolean requesterProxy) {
    this.highTide = highTide;
    this.lowTide = lowTide;
    this.requesterProxy = requesterProxy;
  }

  @Override
  public Channel decorate(Channel socket) {
    return requesterProxy ? new RequesterChannel(socket) : new ResponderChannel(socket);
  }

  /**
   * Create an interceptor for an {@code Channel} that handles request-stream and/or request-channel
   * interactions.
   *
   * @param prefetchRate the prefetch rate to pass to {@link Flux#limitRate(int)}
   * @return the created interceptor
   */
  public static RateLimitInterceptor forResponder(int prefetchRate) {
    return forResponder(prefetchRate, prefetchRate);
  }

  /**
   * Create an interceptor for an {@code Channel} that handles request-stream and/or request-channel
   * interactions with more control over the overall prefetch rate and replenish threshold.
   *
   * @param highTide the high tide value to pass to {@link Flux#limitRate(int, int)}
   * @param lowTide the low tide value to pass to {@link Flux#limitRate(int, int)}
   * @return the created interceptor
   */
  public static RateLimitInterceptor forResponder(int highTide, int lowTide) {
    return new RateLimitInterceptor(highTide, lowTide, false);
  }

  /**
   * Create an interceptor for an {@code Channel} that performs request-channel interactions.
   *
   * @param prefetchRate the prefetch rate to pass to {@link Flux#limitRate(int)}
   * @return the created interceptor
   */
  public static RateLimitInterceptor forRequester(int prefetchRate) {
    return forRequester(prefetchRate, prefetchRate);
  }

  /**
   * Create an interceptor for an {@code Channel} that performs request-channel interactions with
   * more control over the overall prefetch rate and replenish threshold.
   *
   * @param highTide the high tide value to pass to {@link Flux#limitRate(int, int)}
   * @param lowTide the low tide value to pass to {@link Flux#limitRate(int, int)}
   * @return the created interceptor
   */
  public static RateLimitInterceptor forRequester(int highTide, int lowTide) {
    return new RateLimitInterceptor(highTide, lowTide, true);
  }

  /**
   * Responder side proxy, limits response streams.
   */
  private class ResponderChannel extends ChannelDecorator {

    ResponderChannel(Channel source) {
      super(source);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      return delegate.requestStream(payload).limitRate(highTide, lowTide);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      return delegate.requestChannel(payloads).limitRate(highTide, lowTide);
    }
  }

  /**
   * Requester side proxy, limits channel request stream.
   */
  private class RequesterChannel extends ChannelDecorator {

    RequesterChannel(Channel source) {
      super(source);
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      return delegate.requestChannel(Flux.from(payloads).limitRate(highTide, lowTide));
    }

  }
}
