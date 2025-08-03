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

package infra.remoting.transport.local;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

final class LocalSocketAddressTests {

  @DisplayName("constructor throws NullPointerException with null name")
  @Test
  void constructorNullName() {
    assertThatNullPointerException()
            .isThrownBy(() -> new LocalSocketAddress(null))
            .withMessage("name is required");
  }

  @DisplayName("returns the configured name")
  @Test
  void name() {
    assertThat(new LocalSocketAddress("test-name").getName()).isEqualTo("test-name");
  }
}
