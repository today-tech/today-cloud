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
package infra.remoting.test.util;

import java.net.SocketAddress;
import java.util.Objects;

public final class TestLocalSocketAddress extends SocketAddress {

  private static final long serialVersionUID = 2608695156052100164L;

  private final String name;

  /**
   * Creates a new instance.
   *
   * @param name the name representing the address
   * @throws NullPointerException if {@code name} is {@code null}
   */
  public TestLocalSocketAddress(String name) {
    this.name = Objects.requireNonNull(name, "name is required");
  }

  /** Return the name for this connection. */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "[local address] " + name;
  }
}
