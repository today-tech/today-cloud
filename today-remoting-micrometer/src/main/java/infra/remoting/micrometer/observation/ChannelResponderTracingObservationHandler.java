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
