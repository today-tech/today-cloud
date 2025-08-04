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

import infra.remoting.DuplexConnection;
import infra.remoting.frame.FrameType;
import infra.remoting.plugins.ConnectionDecorator;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * An implementation of {@link ConnectionDecorator} that intercepts frames and gathers
 * Micrometer metrics about them.
 *
 * <p>The metric is called {@code infra.remoting.frame} and is tagged with {@code connection.type} ({@link
 * Type}), {@code frame.type} ({@link FrameType}), and any additional configured tags. {@code
 * infra.remoting.duplex.connection.close} and {@code infra.remoting.duplex.connection.dispose} metrics, tagged
 * with {@code connection.type} ({@link Type}) and any additional configured tags are also
 * collected.
 *
 * @see <a href="https://micrometer.io">Micrometer</a>
 */
public final class MicrometerDuplexConnectionDecorator implements ConnectionDecorator {

  private final MeterRegistry meterRegistry;

  private final Tag[] tags;

  /**
   * Creates a new {@link ConnectionDecorator}.
   *
   * @param meterRegistry the {@link MeterRegistry} to use to create {@link Meter}s.
   * @param tags the additional tags to attach to each {@link Meter}
   * @throws NullPointerException if {@code meterRegistry} is {@code null}
   */
  public MicrometerDuplexConnectionDecorator(MeterRegistry meterRegistry, Tag... tags) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry is required");
    this.tags = tags;
  }

  @Override
  public DuplexConnection decorate(Type connectionType, DuplexConnection delegate) {
    Objects.requireNonNull(delegate, "delegate is required");
    Objects.requireNonNull(connectionType, "connectionType is required");

    return new MicrometerDuplexConnection(connectionType, delegate, meterRegistry, tags);
  }
}
