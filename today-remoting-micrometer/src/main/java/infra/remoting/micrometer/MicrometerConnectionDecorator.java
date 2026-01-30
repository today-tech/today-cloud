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

import infra.remoting.Connection;
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
public final class MicrometerConnectionDecorator implements ConnectionDecorator {

  private final MeterRegistry meterRegistry;

  private final Tag[] tags;

  /**
   * Creates a new {@link ConnectionDecorator}.
   *
   * @param meterRegistry the {@link MeterRegistry} to use to create {@link Meter}s.
   * @param tags the additional tags to attach to each {@link Meter}
   * @throws NullPointerException if {@code meterRegistry} is {@code null}
   */
  public MicrometerConnectionDecorator(MeterRegistry meterRegistry, Tag... tags) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry is required");
    this.tags = tags;
  }

  @Override
  public Connection decorate(Type connectionType, Connection delegate) {
    Objects.requireNonNull(delegate, "delegate is required");
    Objects.requireNonNull(connectionType, "connectionType is required");

    return new MicrometerConnection(connectionType, delegate, meterRegistry, tags);
  }
}
