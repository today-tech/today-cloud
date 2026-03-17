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

package infra.remoting.micrometer.observation;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import infra.remoting.Channel;
import infra.remoting.DecoratingChannel;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import io.micrometer.common.util.StringUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tracing representation of a {@link DecoratingChannel} for the responder.
 *
 * @author Marcin Grzejszczak
 * @author Oleh Dokuka
 */
public class ObservationResponderChannel extends DecoratingChannel {
  /** Aligned with ObservationThreadLocalAccessor#KEY */
  private static final String MICROMETER_OBSERVATION_KEY = "micrometer.observation";

  private final ObservationRegistry observationRegistry;

  @Nullable
  private final ChannelResponderObservationConvention observationConvention;

  public ObservationResponderChannel(Channel source, ObservationRegistry observationRegistry) {
    this(source, observationRegistry, null);
  }

  public ObservationResponderChannel(Channel source,
          ObservationRegistry observationRegistry,
          @Nullable ChannelResponderObservationConvention observationConvention) {
    super(source);
    this.observationRegistry = observationRegistry;
    this.observationConvention = observationConvention;
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    // called on Netty EventLoop
    // there can't be observation in thread local here
    ByteBuf sliceMetadata = payload.sliceMetadata();
    String route = route(payload, sliceMetadata);
    ChannelContext channelContext = new ChannelContext(payload,
            payload.sliceMetadata(), FrameType.REQUEST_FNF, route,
            ChannelContext.Side.RESPONDER);
    Observation newObservation = startObservation(RemotingObservationDocumentation.RESPONDER_FNF, channelContext);
    return super.fireAndForget(channelContext.modifiedPayload)
            .doOnError(newObservation::error)
            .doFinally(signalType -> newObservation.stop())
            .contextWrite(context -> context.put(MICROMETER_OBSERVATION_KEY, newObservation));
  }

  private Observation startObservation(
          RemotingObservationDocumentation observation, ChannelContext channelContext) {
    return observation.start(this.observationConvention,
            new DefaultChannelResponderObservationConvention(channelContext),
            () -> channelContext, this.observationRegistry);
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    ByteBuf sliceMetadata = payload.sliceMetadata();
    String route = route(payload, sliceMetadata);
    ChannelContext channelContext =
            new ChannelContext(
                    payload,
                    payload.sliceMetadata(),
                    FrameType.REQUEST_RESPONSE,
                    route,
                    ChannelContext.Side.RESPONDER);
    Observation newObservation =
            startObservation(
                    RemotingObservationDocumentation.RESPONDER_REQUEST_RESPONSE, channelContext);
    return super.requestResponse(channelContext.modifiedPayload)
            .doOnError(newObservation::error)
            .doFinally(signalType -> newObservation.stop())
            .contextWrite(context -> context.put(MICROMETER_OBSERVATION_KEY, newObservation));
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    ByteBuf sliceMetadata = payload.sliceMetadata();
    String route = route(payload, sliceMetadata);
    ChannelContext channelContext =
            new ChannelContext(
                    payload, sliceMetadata, FrameType.REQUEST_STREAM, route, ChannelContext.Side.RESPONDER);
    Observation newObservation =
            startObservation(
                    RemotingObservationDocumentation.RESPONDER_REQUEST_STREAM, channelContext);
    return super.requestStream(channelContext.modifiedPayload)
            .doOnError(newObservation::error)
            .doFinally(signalType -> newObservation.stop())
            .contextWrite(context -> context.put(MICROMETER_OBSERVATION_KEY, newObservation));
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return Flux.from(payloads)
            .switchOnFirst(
                    (firstSignal, flux) -> {
                      final Payload firstPayload = firstSignal.get();
                      if (firstPayload != null) {
                        ByteBuf sliceMetadata = firstPayload.sliceMetadata();
                        String route = route(firstPayload, sliceMetadata);
                        ChannelContext channelContext =
                                new ChannelContext(
                                        firstPayload,
                                        firstPayload.sliceMetadata(),
                                        FrameType.REQUEST_CHANNEL,
                                        route,
                                        ChannelContext.Side.RESPONDER);
                        Observation newObservation =
                                startObservation(
                                        RemotingObservationDocumentation.RESPONDER_REQUEST_CHANNEL,
                                        channelContext);
                        if (StringUtils.isNotBlank(route)) {
                          newObservation.contextualName(channelContext.frameType.name() + " " + route);
                        }
                        return super.requestChannel(flux.skip(1).startWith(channelContext.modifiedPayload))
                                .doOnError(newObservation::error)
                                .doFinally(signalType -> newObservation.stop())
                                .contextWrite(
                                        context -> context.put(MICROMETER_OBSERVATION_KEY, newObservation));
                      }
                      return flux;
                    });
  }

  private String route(Payload payload, ByteBuf headers) {
    return null;
  }
}
