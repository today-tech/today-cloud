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

package infra.remoting.test;

import java.util.function.Predicate;

import infra.remoting.Payload;

import static infra.remoting.test.TransportTest.logger;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/2 17:42
 */
public class PayloadPredicate implements Predicate<Payload> {
  final int expectedCnt;
  int cnt;

  public PayloadPredicate(int expectedCnt) {
    this.expectedCnt = expectedCnt;
  }

  @Override
  public boolean test(Payload p) {
    boolean shouldConsume = cnt++ < expectedCnt;
    if (!shouldConsume) {
      logger.info("Metadata: \n\r{}\n\rData:{}",
              p.hasMetadata()
                      ? new ByteBufRepresentation().fallbackToStringOf(p.sliceMetadata())
                      : "Empty",
              new ByteBufRepresentation().fallbackToStringOf(p.sliceData()));
    }
    return shouldConsume;
  }
}