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