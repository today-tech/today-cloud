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
