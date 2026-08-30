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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import infra.remoting.Connection;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static infra.remoting.plugins.ConnectionDecorator.Type.CLIENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;

final class MicrometerConnectionDecoratorTests {

  private final Connection delegate = mock(Connection.class, RETURNS_SMART_NULLS);

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  @DisplayName("creates MicrometerConnection")
  @Test
  void apply() {
    assertThat(new MicrometerConnectionDecorator(meterRegistry).decorate(CLIENT, delegate))
            .isInstanceOf(MicrometerConnection.class);
  }

  @DisplayName("apply throws NullPointerException with null connectionType")
  @Test
  void applyNullConnectionType() {
    assertThatNullPointerException()
            .isThrownBy(
                    () -> new MicrometerConnectionDecorator(meterRegistry).decorate(null, delegate))
            .withMessage("connectionType is required");
  }

  @DisplayName("apply throws NullPointerException with null delegate")
  @Test
  void applyNullDelegate() {
    assertThatNullPointerException()
            .isThrownBy(
                    () -> new MicrometerConnectionDecorator(meterRegistry).decorate(CLIENT, null))
            .withMessage("delegate is required");
  }

  @DisplayName("constructor throws NullPointer exception with null meterRegistry")
  @Test
  void constructorNullMeterRegistry() {
    assertThatNullPointerException()
            .isThrownBy(() -> new MicrometerConnectionDecorator(null))
            .withMessage("meterRegistry is required");
  }
}
