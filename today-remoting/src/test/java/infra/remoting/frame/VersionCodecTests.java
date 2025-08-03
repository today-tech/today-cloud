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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VersionCodecTests {
  @Test
  public void simple() {
    int version = VersionCodec.encode(1, 0);
    assertEquals(1, VersionCodec.major(version));
    assertEquals(0, VersionCodec.minor(version));
    assertEquals(0x00010000, version);
    assertEquals("1.0", VersionCodec.toString(version));
  }

  @Test
  public void complex() {
    int version = VersionCodec.encode(0x1234, 0x5678);
    assertEquals(0x1234, VersionCodec.major(version));
    assertEquals(0x5678, VersionCodec.minor(version));
    assertEquals(0x12345678, version);
    assertEquals("4660.22136", VersionCodec.toString(version));
  }

  @Test
  public void noShortOverflow() {
    int version = VersionCodec.encode(43210, 43211);
    assertEquals(43210, VersionCodec.major(version));
    assertEquals(43211, VersionCodec.minor(version));
  }
}
