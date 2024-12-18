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

package infra.config;

import java.util.function.Supplier;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/14 21:43
 */
public class DynamicProperty<T> implements Supplier<T> {

  private T value;

  @Nullable
  @Override
  public T get() {
    return value;
  }

  public T obtain() {
    T value = get();
    Assert.state(value != null, "Value not set");
    return value;
  }

  private void update(T value) {
    this.value = value;
  }

}
