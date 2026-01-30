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
import io.micrometer.observation.Observation;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import io.netty.buffer.ByteBuf;

public class ChannelRequesterTracingObservationHandler implements TracingObservationHandler<ChannelContext> {

  private static final Logger log = LoggerFactory.getLogger(ChannelRequesterTracingObservationHandler.class);

  private final Propagator propagator;

  private final Propagator.Setter<ByteBuf> setter;

  private final Tracer tracer;

  public ChannelRequesterTracingObservationHandler(Tracer tracer,
          Propagator propagator, Propagator.Setter<ByteBuf> setter) {
    this.tracer = tracer;
    this.propagator = propagator;
    this.setter = setter;
  }

  @Override
  public boolean supportsContext(Observation.Context context) {
    return context instanceof ChannelContext
            && ((ChannelContext) context).side == ChannelContext.Side.REQUESTER;
  }

  @Override
  public Tracer getTracer() {
    return this.tracer;
  }

  @Override
  public void onStart(ChannelContext context) {
    Payload payload = context.payload;
    Span.Builder spanBuilder = this.tracer.spanBuilder();
    Span parentSpan = getParentSpan(context);
    if (parentSpan != null) {
      spanBuilder.setParent(parentSpan.context());
    }
    Span span = spanBuilder.kind(Span.Kind.PRODUCER).start();
    log.debug("Extracted result from context or thread local {}", span);

    final ByteBuf newMetadata = PayloadUtils.cleanTracingMetadata(payload, new HashSet<>(propagator.fields()));
    TraceContext traceContext = span.context();
    this.propagator.inject(traceContext, newMetadata, this.setter);
    context.modifiedPayload = PayloadUtils.payload(payload, newMetadata);
    getTracingContext(context).setSpan(span);
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
    span.name(context.getContextualName()).end();
  }

}
