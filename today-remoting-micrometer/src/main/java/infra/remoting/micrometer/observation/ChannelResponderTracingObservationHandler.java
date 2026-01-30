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

import java.util.HashSet;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import io.micrometer.observation.Observation;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import io.netty.buffer.ByteBuf;

public class ChannelResponderTracingObservationHandler implements TracingObservationHandler<ChannelContext> {

  private static final Logger log = LoggerFactory.getLogger(ChannelResponderTracingObservationHandler.class);

  private final Propagator propagator;

  private final Propagator.Getter<ByteBuf> getter;

  private final Tracer tracer;

  public ChannelResponderTracingObservationHandler(Tracer tracer, Propagator propagator, Propagator.Getter<ByteBuf> getter) {
    this.tracer = tracer;
    this.propagator = propagator;
    this.getter = getter;
  }

  @Override
  public void onStart(ChannelContext context) {
    Span handle = consumerSpanBuilder(context.payload, context.metadata, context.frameType);
    ByteBuf bufs = PayloadUtils.cleanTracingMetadata(context.payload, new HashSet<>(propagator.fields()));
    context.modifiedPayload = PayloadUtils.payload(context.payload, bufs);
    getTracingContext(context).setSpan(handle);
  }

  @Override
  public void onError(ChannelContext context) {
    Throwable error = context.getError();
    if (error != null) {
      getRequiredSpan(context).error(error);
    }
  }

  @Override
  public void onStop(ChannelContext context) {
    Span span = getRequiredSpan(context);
    tagSpan(context, span);
    span.end();
  }

  @Override
  public boolean supportsContext(Observation.Context context) {
    return context instanceof ChannelContext
            && ((ChannelContext) context).side == ChannelContext.Side.RESPONDER;
  }

  @Override
  public Tracer getTracer() {
    return this.tracer;
  }

  private Span consumerSpanBuilder(Payload payload, ByteBuf headers, FrameType requestType) {
    Span.Builder consumerSpanBuilder = consumerSpanBuilder(payload, headers);
    log.debug("Extracted result from headers {}", consumerSpanBuilder);
    String name = "handle";
    return consumerSpanBuilder.kind(Span.Kind.CONSUMER).name(name).start();
  }

  private Span.Builder consumerSpanBuilder(Payload payload, ByteBuf headers) {
    return this.propagator.extract(headers, this.getter);
  }
}
