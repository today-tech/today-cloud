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

import infra.remoting.Channel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;

final class MicrometerChannelDecoratorTests {

  private final Channel delegate = mock(Channel.class, RETURNS_SMART_NULLS);

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  @DisplayName("creates Micrometer")
  @Test
  void apply() {
    assertThat(new MicrometerChannelDecorator(meterRegistry).decorate(delegate))
            .isInstanceOf(MicrometerChannel.class);
  }

  @DisplayName("apply throws NullPointerException with null delegate")
  @Test
  void applyNullDelegate() {
    assertThatNullPointerException()
            .isThrownBy(() -> new MicrometerChannelDecorator(meterRegistry).decorate(null))
            .withMessage("delegate is required");
  }

  @DisplayName("constructor throws NullPointerException with null meterRegistry")
  @Test
  void constructorNullMeterRegistry() {
    assertThatNullPointerException()
            .isThrownBy(() -> new MicrometerChannelDecorator(null))
            .withMessage("meterRegistry is required");
  }
}
