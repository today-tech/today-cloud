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

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;

import java.util.Queue;
import java.util.function.Consumer;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import infra.remoting.frame.FrameType;
import infra.remoting.internal.jctools.queues.MpscUnboundedArrayQueue;

public class TestRequestInterceptor implements RequestInterceptor {

  final Queue<Event> events = new MpscUnboundedArrayQueue<>(128);

  @Override
  public void dispose() { }

  @Override
  public void onStart(int streamId, FrameType requestType, @Nullable ByteBuf metadata) {
    events.add(new Event(EventType.ON_START, streamId, requestType, null));
  }

  @Override
  public void onTerminate(int streamId, FrameType requestType, @Nullable Throwable t) {
    events.add(
            new Event(
                    t == null ? EventType.ON_COMPLETE : EventType.ON_ERROR, streamId, requestType, t));
  }

  @Override
  public void onCancel(int streamId, FrameType requestType) {
    events.add(new Event(EventType.ON_CANCEL, streamId, requestType, null));
  }

  @Override
  public void onReject(
          Throwable rejectionReason, FrameType requestType, @Nullable ByteBuf metadata) {
    events.add(new Event(EventType.ON_REJECT, -1, requestType, rejectionReason));
  }

  public TestRequestInterceptor expectOnStart(int streamId, FrameType requestType) {
    final Event event = events.poll();

    Assertions.assertThat(event)
            .hasFieldOrPropertyWithValue("eventType", EventType.ON_START)
            .hasFieldOrPropertyWithValue("streamId", streamId)
            .hasFieldOrPropertyWithValue("requestType", requestType);

    return this;
  }

  public TestRequestInterceptor expectOnComplete(int streamId) {
    final Event event = events.poll();

    Assertions.assertThat(event)
            .hasFieldOrPropertyWithValue("eventType", EventType.ON_COMPLETE)
            .hasFieldOrPropertyWithValue("streamId", streamId);

    return this;
  }

  public TestRequestInterceptor expectOnError(int streamId) {
    final Event event = events.poll();

    Assertions.assertThat(event)
            .hasFieldOrPropertyWithValue("eventType", EventType.ON_ERROR)
            .hasFieldOrPropertyWithValue("streamId", streamId);

    return this;
  }

  public TestRequestInterceptor expectOnCancel(int streamId) {
    final Event event = events.poll();

    Assertions.assertThat(event)
            .hasFieldOrPropertyWithValue("eventType", EventType.ON_CANCEL)
            .hasFieldOrPropertyWithValue("streamId", streamId);

    return this;
  }

  public TestRequestInterceptor assertNext(Consumer<Event> consumer) {
    final Event event = events.poll();
    Assertions.assertThat(event).isNotNull();

    consumer.accept(event);

    return this;
  }

  public TestRequestInterceptor expectOnReject(FrameType requestType, Throwable rejectionReason) {
    final Event event = events.poll();

    Assertions.assertThat(event)
            .hasFieldOrPropertyWithValue("eventType", EventType.ON_REJECT)
            .has(
                    new Condition<>(
                            e -> {
                              Assertions.assertThat(e.error)
                                      .isExactlyInstanceOf(rejectionReason.getClass())
                                      .hasMessage(rejectionReason.getMessage())
                                      .hasCause(rejectionReason.getCause());
                              return true;
                            },
                            "Has rejection reason which matches to %s",
                            rejectionReason))
            .hasFieldOrPropertyWithValue("requestType", requestType);

    return this;
  }

  public TestRequestInterceptor expectNothing() {
    final Event event = events.poll();

    Assertions.assertThat(event).isNull();

    return this;
  }

  public static final class Event {
    public final EventType eventType;
    public final int streamId;
    public final FrameType requestType;
    public final Throwable error;

    Event(EventType eventType, int streamId, FrameType requestType, Throwable error) {
      this.eventType = eventType;
      this.streamId = streamId;
      this.requestType = requestType;
      this.error = error;
    }
  }

  public enum EventType {
    ON_START,
    ON_COMPLETE,
    ON_ERROR,
    ON_CANCEL,
    ON_REJECT
  }
}
