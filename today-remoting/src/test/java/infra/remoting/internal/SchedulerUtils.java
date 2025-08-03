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

package infra.remoting.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import reactor.core.scheduler.Scheduler;

public class SchedulerUtils {

  public static void warmup(Scheduler scheduler) throws InterruptedException {
    warmup(scheduler, 10000);
  }

  public static void warmup(Scheduler scheduler, int times) throws InterruptedException {
    scheduler.start();

    // warm up
    CountDownLatch latch = new CountDownLatch(times);
    for (int i = 0; i < times; i++) {
      scheduler.schedule(latch::countDown);
    }
    latch.await(5, TimeUnit.SECONDS);
  }
}
