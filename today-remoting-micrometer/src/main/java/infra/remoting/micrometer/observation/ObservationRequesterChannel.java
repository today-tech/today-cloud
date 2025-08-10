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

package infra.remoting.micrometer.observation;

import org.reactivestreams.Publisher;

import java.util.function.Function;

import infra.lang.Nullable;
import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import infra.remoting.ChannelWrapper;
import io.micrometer.common.util.StringUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.docs.ObservationDocumentation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * Tracing representation of a {@link ChannelWrapper} for the requester.
 *
 * @author Marcin Grzejszczak
 * @author Oleh Dokuka
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class ObservationRequesterChannel extends ChannelWrapper {

  /**
   * Aligned with ObservationThreadLocalAccessor#KEY
   */
  private static final String MICROMETER_OBSERVATION_KEY = "micrometer.observation";

  private final ObservationRegistry observationRegistry;

  @Nullable
  private final ChannelRequesterObservationConvention observationConvention;

  public ObservationRequesterChannel(Channel source, ObservationRegistry observationRegistry) {
    this(source, observationRegistry, null);
  }

  public ObservationRequesterChannel(Channel source, ObservationRegistry observationRegistry,
          @Nullable ChannelRequesterObservationConvention observationConvention) {
    super(source);
    this.observationRegistry = observationRegistry;
    this.observationConvention = observationConvention;
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    return setObservation(
            super::fireAndForget,
            payload,
            FrameType.REQUEST_FNF,
            RemotingObservationDocumentation.REQUESTER_FNF);
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    return setObservation(
            super::requestResponse,
            payload,
            FrameType.REQUEST_RESPONSE,
            RemotingObservationDocumentation.REQUESTER_REQUEST_RESPONSE);
  }

  <T> Mono<T> setObservation(Function<Payload, Mono<T>> input, Payload payload,
          FrameType frameType, ObservationDocumentation observation) {
    return Mono.deferContextual(
            contextView -> observe(input, payload, frameType, observation, contextView));
  }

  private String route(Payload payload) {
    return null;
  }

  private <T> Mono<T> observe(
          Function<Payload, Mono<T>> input,
          Payload payload,
          FrameType frameType,
          ObservationDocumentation obs,
          ContextView contextView) {
    String route = route(payload);
    ChannelContext channelContext =
            new ChannelContext(
                    payload, payload.sliceMetadata(), frameType, route, ChannelContext.Side.REQUESTER);
    Observation parentObservation = contextView.getOrDefault(MICROMETER_OBSERVATION_KEY, null);
    Observation observation = obs.observation(this.observationConvention,
                    new DefaultChannelRequesterObservationConvention(channelContext),
                    () -> channelContext, observationRegistry)
            .parentObservation(parentObservation);
    setContextualName(frameType, route, observation);
    observation.start();
    Payload newPayload = payload;
    if (channelContext.modifiedPayload != null) {
      newPayload = channelContext.modifiedPayload;
    }
    return input
            .apply(newPayload)
            .doOnError(observation::error)
            .doFinally(signalType -> observation.stop())
            .contextWrite(context -> context.put(MICROMETER_OBSERVATION_KEY, observation));
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    return observationFlux(
            super::requestStream,
            payload,
            FrameType.REQUEST_STREAM,
            RemotingObservationDocumentation.REQUESTER_REQUEST_STREAM);
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> inbound) {
    return Flux.from(inbound)
            .switchOnFirst(
                    (firstSignal, flux) -> {
                      final Payload firstPayload = firstSignal.get();
                      if (firstPayload != null) {
                        return observationFlux(
                                p -> super.requestChannel(flux.skip(1).startWith(p)),
                                firstPayload,
                                FrameType.REQUEST_CHANNEL,
                                RemotingObservationDocumentation.REQUESTER_REQUEST_CHANNEL);
                      }
                      return flux;
                    });
  }

  private Flux<Payload> observationFlux(Function<Payload, Flux<Payload>> input,
          Payload payload, FrameType frameType, ObservationDocumentation obs) {
    return Flux.deferContextual(contextView -> {
      String route = route(payload);
      ChannelContext channelContext =
              new ChannelContext(
                      payload,
                      payload.sliceMetadata(),
                      frameType,
                      route,
                      ChannelContext.Side.REQUESTER);
      Observation parentObservation =
              contextView.getOrDefault(MICROMETER_OBSERVATION_KEY, null);
      Observation newObservation =
              obs.observation(this.observationConvention,
                              new DefaultChannelRequesterObservationConvention(channelContext),
                              () -> channelContext, this.observationRegistry)
                      .parentObservation(parentObservation);
      setContextualName(frameType, route, newObservation);
      newObservation.start();
      return input
              .apply(channelContext.modifiedPayload)
              .doOnError(newObservation::error)
              .doFinally(signalType -> newObservation.stop())
              .contextWrite(context -> context.put(MICROMETER_OBSERVATION_KEY, newObservation));
    });
  }

  private void setContextualName(FrameType frameType, String route, Observation newObservation) {
    if (StringUtils.isNotBlank(route)) {
      newObservation.contextualName(frameType.name() + " " + route);
    }
    else {
      newObservation.contextualName(frameType.name());
    }
  }
}
