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

package io.rsocket.test;

import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;

import io.rsocket.Payload;
import reactor.core.publisher.BaseSubscriber;

class CountdownBaseSubscriber extends BaseSubscriber<Payload> {
  private CountDownLatch latch = new CountDownLatch(0);
  private int count = 0;

  public void expect(int count) {
    latch = new CountDownLatch((int) latch.getCount() + count);
    if (upstream() != null) {
      request(count);
    }
  }

  @Override
  protected void hookOnNext(Payload value) {
    count++;
    latch.countDown();
  }

  @Override
  protected void hookOnSubscribe(Subscription subscription) {
    long count = latch.getCount();

    if (count > 0) {
      subscription.request(count);
    }
  }

  public void await() {
    try {
      latch.await();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public int count() {
    return count;
  }
}
