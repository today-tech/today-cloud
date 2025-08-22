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

package infra.remoting.micrometer;

import org.reactivestreams.Publisher;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import infra.remoting.Channel;
import infra.remoting.ChannelWrapper;
import infra.remoting.Payload;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import static reactor.core.publisher.SignalType.CANCEL;
import static reactor.core.publisher.SignalType.ON_COMPLETE;
import static reactor.core.publisher.SignalType.ON_ERROR;

/**
 * An implementation of {@link Channel} that intercepts interactions and gathers Micrometer metrics
 * about them.
 *
 * <p>The metrics are called {@code infra.remoting.[ metadata.push | request.channel | request.fnf |
 * request.response | request.stream ]} and is tagged with {@code signal.type} ({@link SignalType})
 * and any additional configured tags.
 *
 * @see <a href="https://micrometer.io">Micrometer</a>
 */
final class MicrometerChannel extends ChannelWrapper {

  private final InteractionCounters metadataPush;

  private final InteractionCounters requestChannel;

  private final InteractionCounters requestFireAndForget;

  private final InteractionTimers requestResponse;

  private final InteractionCounters requestStream;

  /**
   * Creates a new {@link Channel}.
   *
   * @param delegate the {@link Channel} to delegate to
   * @param meterRegistry the {@link MeterRegistry} to use
   * @param tags additional tags to attach to {@link Meter}s
   * @throws NullPointerException if {@code delegate} or {@code meterRegistry} is {@code null}
   */
  MicrometerChannel(Channel delegate, MeterRegistry meterRegistry, Tag... tags) {
    super(delegate);
    Objects.requireNonNull(meterRegistry, "meterRegistry is required");

    this.metadataPush = new InteractionCounters(meterRegistry, "metadata.push", tags);
    this.requestChannel = new InteractionCounters(meterRegistry, "request.channel", tags);
    this.requestFireAndForget = new InteractionCounters(meterRegistry, "request.fnf", tags);
    this.requestResponse = new InteractionTimers(meterRegistry, "request.response", tags);
    this.requestStream = new InteractionCounters(meterRegistry, "request.stream", tags);
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    return delegate.fireAndForget(payload).doFinally(requestFireAndForget);
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    return delegate.metadataPush(payload).doFinally(metadataPush);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return delegate.requestChannel(payloads).doFinally(requestChannel);
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    return Mono.defer(() -> {
      Sample sample = requestResponse.start();
      return delegate
              .requestResponse(payload)
              .doFinally(signalType -> requestResponse.accept(sample, signalType));
    });
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    return delegate.requestStream(payload).doFinally(requestStream);
  }

  private static final class InteractionCounters implements Consumer<SignalType> {

    private final Counter cancel;

    private final Counter onComplete;

    private final Counter onError;

    private InteractionCounters(MeterRegistry meterRegistry, String interactionModel, Tag... tags) {
      this.cancel = counter(meterRegistry, interactionModel, CANCEL, tags);
      this.onComplete = counter(meterRegistry, interactionModel, ON_COMPLETE, tags);
      this.onError = counter(meterRegistry, interactionModel, ON_ERROR, tags);
    }

    @Override
    public void accept(SignalType signalType) {
      switch (signalType) {
        case CANCEL:
          cancel.increment();
          break;
        case ON_COMPLETE:
          onComplete.increment();
          break;
        case ON_ERROR:
          onError.increment();
          break;
      }
    }

    private static Counter counter(
            MeterRegistry meterRegistry, String interactionModel, SignalType signalType, Tag... tags) {

      return meterRegistry.counter(
              "infra.remoting." + interactionModel, Tags.of(tags).and("signal.type", signalType.name()));
    }
  }

  private static final class InteractionTimers implements BiConsumer<Sample, SignalType> {

    private final Timer cancel;

    private final MeterRegistry meterRegistry;

    private final Timer onComplete;

    private final Timer onError;

    private InteractionTimers(MeterRegistry meterRegistry, String interactionModel, Tag... tags) {
      this.meterRegistry = meterRegistry;

      this.cancel = timer(meterRegistry, interactionModel, CANCEL, tags);
      this.onComplete = timer(meterRegistry, interactionModel, ON_COMPLETE, tags);
      this.onError = timer(meterRegistry, interactionModel, ON_ERROR, tags);
    }

    @Override
    public void accept(Sample sample, SignalType signalType) {
      switch (signalType) {
        case CANCEL:
          sample.stop(cancel);
          break;
        case ON_COMPLETE:
          sample.stop(onComplete);
          break;
        case ON_ERROR:
          sample.stop(onError);
          break;
      }
    }

    Sample start() {
      return Timer.start(meterRegistry);
    }

    private static Timer timer(MeterRegistry meterRegistry,
            String interactionModel, SignalType signalType, Tag... tags) {

      return meterRegistry.timer(
              "infra.remoting." + interactionModel, Tags.of(tags).and("signal.type", signalType.name()));
    }
  }
}
