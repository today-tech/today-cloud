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

package infra.remoting.micrometer;

import java.util.Objects;

import infra.remoting.Channel;
import infra.remoting.plugins.ChannelDecorator;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import reactor.core.publisher.SignalType;

/**
 * An implementation of {@link ChannelDecorator} that intercepts interactions and gathers
 * Micrometer metrics about them.
 *
 * <p>The metrics are called {@code infra.remoting.[ metadata.push | request.channel | request.fnf |
 * request.response | request.stream ]} and is tagged with {@code signal.type} ({@link SignalType})
 * and any additional configured tags.
 *
 * @see <a href="https://micrometer.io">Micrometer</a>
 */
public final class MicrometerChannelDecorator implements ChannelDecorator {

  private final MeterRegistry meterRegistry;

  private final Tag[] tags;

  /**
   * Creates a new {@link ChannelDecorator}.
   *
   * @param meterRegistry the {@link MeterRegistry} to use to create {@link Meter}s.
   * @param tags the additional tags to attach to each {@link Meter}
   * @throws NullPointerException if {@code meterRegistry} is {@code null}
   */
  public MicrometerChannelDecorator(MeterRegistry meterRegistry, Tag... tags) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry is required");
    this.tags = tags;
  }

  @Override
  public Channel decorate(Channel delegate) {
    Objects.requireNonNull(delegate, "delegate is required");
    return new MicrometerChannel(delegate, meterRegistry, tags);
  }
}
