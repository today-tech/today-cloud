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

package infra.remoting.frame;

public class VersionCodec {

  public static int encode(int major, int minor) {
    return (major << 16) | (minor & 0xFFFF);
  }

  public static int major(int version) {
    return version >> 16 & 0xFFFF;
  }

  public static int minor(int version) {
    return version & 0xFFFF;
  }

  public static String toString(int version) {
    return major(version) + "." + minor(version);
  }
}
