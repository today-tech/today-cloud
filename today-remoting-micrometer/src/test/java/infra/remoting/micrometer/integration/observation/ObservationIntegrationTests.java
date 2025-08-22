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

package infra.remoting.micrometer.integration.observation;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.micrometer.observation.ChannelRequesterTracingObservationHandler;
import infra.remoting.micrometer.observation.ChannelResponderTracingObservationHandler;
import infra.remoting.micrometer.observation.ObservationRequesterChannel;
import infra.remoting.micrometer.observation.ObservationResponderChannel;
import infra.remoting.plugins.ChannelDecorator;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.reporter.BuildingBlocks;
import io.micrometer.tracing.test.simple.SpansAssert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

public class ObservationIntegrationTests extends SampleTestRunner {
  private static final MeterRegistry registry = new SimpleMeterRegistry();
  private static final ObservationRegistry observationRegistry = ObservationRegistry.create();

  static {
    observationRegistry
            .observationConfig()
            .observationHandler(new DefaultMeterObservationHandler(registry));
  }

  private final ChannelDecorator requesterInterceptor;
  private final ChannelDecorator responderInterceptor;

  ObservationIntegrationTests() {
    super(SampleRunnerConfig.builder().build());
    requesterInterceptor = channel -> new ObservationRequesterChannel(channel, observationRegistry);
    responderInterceptor = channel -> new ObservationResponderChannel(channel, observationRegistry);
  }

  private CloseableChannel server;
  private Channel client;
  private AtomicInteger counter;

  @Override
  public BiConsumer<BuildingBlocks, Deque<ObservationHandler<? extends Observation.Context>>> customizeObservationHandlers() {
    return (buildingBlocks, observationHandlers) -> {
      ByteBufSetter setter = new ByteBufSetter();
      observationHandlers.addFirst(
              new ChannelRequesterTracingObservationHandler(
                      buildingBlocks.getTracer(),
                      buildingBlocks.getPropagator(),
                      setter
              ));
      observationHandlers.addFirst(
              new ChannelResponderTracingObservationHandler(
                      buildingBlocks.getTracer(),
                      buildingBlocks.getPropagator(),
                      new ByteBufGetter(setter.map)));
    };
  }

  @AfterEach
  public void teardown() {
    if (server != null) {
      server.dispose();
    }
  }

  private void testRequest() {
    counter.set(0);
    client.requestResponse(DefaultPayload.create("REQUEST", "META")).block();
    assertThat(counter).as("Server did not see the request.").hasValue(1);
  }

  private void testStream() {
    counter.set(0);
    client.requestStream(DefaultPayload.create("start")).blockLast();

    assertThat(counter).as("Server did not see the request.").hasValue(1);
  }

  private void testRequestChannel() {
    counter.set(0);
    client.requestChannel(Mono.just(DefaultPayload.create("start"))).blockFirst();
    assertThat(counter).as("Server did not see the request.").hasValue(1);
  }

  private void testFireAndForget() {
    counter.set(0);
    client.fireAndForget(DefaultPayload.create("start")).subscribe();
    Awaitility.await().atMost(Duration.ofSeconds(50)).until(() -> counter.get() == 1);
    assertThat(counter).as("Server did not see the request.").hasValue(1);
  }

  @Override
  public SampleTestRunnerConsumer yourCode() {
    return (bb, meterRegistry) -> {
      counter = new AtomicInteger();
      server = RemotingServer.create((setup, channel) -> {
                channel.onClose().subscribe();

                return Mono.just(
                        new Channel() {
                          @Override
                          public Mono<Payload> requestResponse(Payload payload) {
                            payload.release();
                            counter.incrementAndGet();
                            return Mono.just(DefaultPayload.create("RESPONSE", "METADATA"));
                          }

                          @Override
                          public Flux<Payload> requestStream(Payload payload) {
                            payload.release();
                            counter.incrementAndGet();
                            return Flux.range(1, 10_000)
                                    .map(i -> DefaultPayload.create("data -> " + i));
                          }

                          @Override
                          public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                            counter.incrementAndGet();
                            return Flux.from(payloads);
                          }

                          @Override
                          public Mono<Void> fireAndForget(Payload payload) {
                            payload.release();
                            counter.incrementAndGet();
                            return Mono.empty();
                          }
                        });
              })
              .interceptors(registry -> registry.forResponder(responderInterceptor))
              .bind(TcpServerTransport.create("localhost", 0))
              .block();

      client = ChannelConnector.create()
              .interceptors(registry -> registry.forRequester(requesterInterceptor))
              .connect(TcpClientTransport.create(server.address()))
              .block();

      testRequest();

      testStream();

      testRequestChannel();

      testFireAndForget();

      SpansAssert.assertThat(bb.getFinishedSpans())
              .haveSameTraceId()
              // "request_*" + "handle" x 4
              .hasNumberOfSpansEqualTo(8)
              .hasNumberOfSpansWithNameEqualTo("handle", 4)
              .forAllSpansWithNameEqualTo("handle", span -> span.hasTagWithKey("infra.remoting.request-type"))
              .hasASpanWithNameIgnoreCase("request_stream")
              .thenASpanWithNameEqualToIgnoreCase("request_stream")
              .hasTag("infra.remoting.request-type", "REQUEST_STREAM")
              .backToSpans()
              .hasASpanWithNameIgnoreCase("request_channel")
              .thenASpanWithNameEqualToIgnoreCase("request_channel")
              .hasTag("infra.remoting.request-type", "REQUEST_CHANNEL")
              .backToSpans()
              .hasASpanWithNameIgnoreCase("request_fnf")
              .thenASpanWithNameEqualToIgnoreCase("request_fnf")
              .hasTag("infra.remoting.request-type", "REQUEST_FNF")
              .backToSpans()
              .hasASpanWithNameIgnoreCase("request_response")
              .thenASpanWithNameEqualToIgnoreCase("request_response")
              .hasTag("infra.remoting.request-type", "REQUEST_RESPONSE");

      MeterRegistryAssert.assertThat(registry)
              .hasTimerWithNameAndTags("infra.remoting.response",
                      Tags.of(Tag.of("error", "none"), Tag.of("infra.remoting.request-type", "REQUEST_RESPONSE")))
              .hasTimerWithNameAndTags("infra.remoting.fnf",
                      Tags.of(Tag.of("error", "none"), Tag.of("infra.remoting.request-type", "REQUEST_FNF")))
              .hasTimerWithNameAndTags("infra.remoting.request",
                      Tags.of(Tag.of("error", "none"), Tag.of("infra.remoting.request-type", "REQUEST_RESPONSE")))
              .hasTimerWithNameAndTags("infra.remoting.channel",
                      Tags.of(Tag.of("error", "none"), Tag.of("infra.remoting.request-type", "REQUEST_CHANNEL")))
              .hasTimerWithNameAndTags("infra.remoting.stream",
                      Tags.of(Tag.of("error", "none"), Tag.of("infra.remoting.request-type", "REQUEST_STREAM")));
    };
  }

  @Override
  protected MeterRegistry getMeterRegistry() {
    return registry;
  }

  @Override
  protected ObservationRegistry getObservationRegistry() {
    return observationRegistry;
  }
}
