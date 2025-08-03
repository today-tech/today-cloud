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

package io.rsocket.test.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class StringUtils {

  private static final String CANDIDATE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

  private StringUtils() { }

  public static String getRandomString(int size) {
    return ThreadLocalRandom.current()
            .ints(size, 0, CANDIDATE_CHARS.length())
            .mapToObj(index -> ((Character) CANDIDATE_CHARS.charAt(index)).toString())
            .collect(Collectors.joining());
  }
}
