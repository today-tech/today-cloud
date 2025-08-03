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