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
