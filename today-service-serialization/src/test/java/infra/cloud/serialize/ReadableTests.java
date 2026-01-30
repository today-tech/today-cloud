/*
 * Copyright 2021 - 2026 the original author or authors.
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

package infra.cloud.serialize;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/18 17:12
 */
class ReadableTests {

  @ParameterizedTest
  @MethodSource("args")
  void list(Readable readable) {
    List<Integer> list = readable.read(Readable::readInt);

    Random random = new Random();
    List<Integer> read = readable.read(() -> random.nextInt());

  }

  @ParameterizedTest
  @MethodSource("args")
  void map(Readable readable) {
    Map<String, String> map = readable.read(Readable::readString, Readable::readString);

  }

  static Stream<Arguments> args() {
//    DefaultByteBufInput input = new DefaultByteBufInput();
    return Stream.of(Arguments.arguments());
  }

}