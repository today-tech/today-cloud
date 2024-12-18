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

package infra.cloud.protocol;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/12/22 22:24
 */
public enum ProtocolVersion {

  CURRENT(0);

  private final byte version;

  ProtocolVersion(int version) {
    this.version = (byte) version;
  }

  public byte asByte() {
    return version;
  }

  public static ProtocolVersion valueOf(byte version) {
    return switch (version) {
      case 0 -> CURRENT;
      default -> throw new ProtocolVersionException("Protocol version: '%s' not supported".formatted(version));
    };
  }

}
