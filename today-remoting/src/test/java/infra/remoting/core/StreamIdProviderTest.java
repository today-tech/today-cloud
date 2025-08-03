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

package infra.remoting.core;

import org.junit.jupiter.api.Test;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StreamIdProviderTest {
  @Test
  public void testClientSequence() {
    IntObjectMap<Object> map = new IntObjectHashMap<>();
    StreamIdProvider s = StreamIdProvider.forClient();
    assertThat(s.nextStreamId(map)).isEqualTo(1);
    assertThat(s.nextStreamId(map)).isEqualTo(3);
    assertThat(s.nextStreamId(map)).isEqualTo(5);
  }

  @Test
  public void testServerSequence() {
    IntObjectMap<Object> map = new IntObjectHashMap<>();
    StreamIdProvider s = StreamIdProvider.forServer();
    assertEquals(2, s.nextStreamId(map));
    assertEquals(4, s.nextStreamId(map));
    assertEquals(6, s.nextStreamId(map));
  }

  @Test
  public void testClientIsValid() {
    IntObjectMap<Object> map = new IntObjectHashMap<>();
    StreamIdProvider s = StreamIdProvider.forClient();

    assertFalse(s.isBeforeOrCurrent(1));
    assertFalse(s.isBeforeOrCurrent(3));

    s.nextStreamId(map);
    assertTrue(s.isBeforeOrCurrent(1));
    assertFalse(s.isBeforeOrCurrent(3));

    s.nextStreamId(map);
    assertTrue(s.isBeforeOrCurrent(3));

    // negative
    assertFalse(s.isBeforeOrCurrent(-1));
    // connection
    assertFalse(s.isBeforeOrCurrent(0));
    // server also accepted (checked externally)
    assertTrue(s.isBeforeOrCurrent(2));
  }

  @Test
  public void testServerIsValid() {
    IntObjectMap<Object> map = new IntObjectHashMap<>();
    StreamIdProvider s = StreamIdProvider.forServer();

    assertFalse(s.isBeforeOrCurrent(2));
    assertFalse(s.isBeforeOrCurrent(4));

    s.nextStreamId(map);
    assertTrue(s.isBeforeOrCurrent(2));
    assertFalse(s.isBeforeOrCurrent(4));

    s.nextStreamId(map);
    assertTrue(s.isBeforeOrCurrent(4));

    // negative
    assertFalse(s.isBeforeOrCurrent(-2));
    // connection
    assertFalse(s.isBeforeOrCurrent(0));
    // client also accepted (checked externally)
    assertTrue(s.isBeforeOrCurrent(1));
  }

  @Test
  public void testWrap() {
    IntObjectMap<Object> map = new IntObjectHashMap<>();
    StreamIdProvider s = new StreamIdProvider(Integer.MAX_VALUE - 3);

    assertEquals(2147483646, s.nextStreamId(map));
    assertEquals(2, s.nextStreamId(map));
    assertEquals(4, s.nextStreamId(map));

    s = new StreamIdProvider(Integer.MAX_VALUE - 2);

    assertEquals(2147483647, s.nextStreamId(map));
    assertEquals(1, s.nextStreamId(map));
    assertEquals(3, s.nextStreamId(map));
  }

  @Test
  public void testSkipFound() {
    IntObjectMap<Object> map = new IntObjectHashMap<>();
    map.put(5, new Object());
    map.put(9, new Object());
    StreamIdProvider s = StreamIdProvider.forClient();
    assertEquals(1, s.nextStreamId(map));
    assertEquals(3, s.nextStreamId(map));
    assertEquals(7, s.nextStreamId(map));
    assertEquals(11, s.nextStreamId(map));
  }
}
