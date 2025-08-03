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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.remoting.internal.jctools.queues;

import infra.remoting.internal.jctools.queues.MessagePassingQueue.Consumer;
import infra.remoting.internal.jctools.queues.MessagePassingQueue.ExitCondition;
import infra.remoting.internal.jctools.queues.MessagePassingQueue.Supplier;
import infra.remoting.internal.jctools.queues.MessagePassingQueue.WaitStrategy;

final class MessagePassingQueueUtil {
  public static <E> int drain(MessagePassingQueue<E> queue, Consumer<E> c, int limit) {
    if (null == c)
      throw new IllegalArgumentException("c is null");
    if (limit < 0)
      throw new IllegalArgumentException("limit is negative: " + limit);
    if (limit == 0)
      return 0;
    E e;
    int i = 0;
    for (; i < limit && (e = queue.relaxedPoll()) != null; i++) {
      c.accept(e);
    }
    return i;
  }

  public static <E> int drain(MessagePassingQueue<E> queue, Consumer<E> c) {
    if (null == c)
      throw new IllegalArgumentException("c is null");
    E e;
    int i = 0;
    while ((e = queue.relaxedPoll()) != null) {
      i++;
      c.accept(e);
    }
    return i;
  }

  public static <E> void drain(
          MessagePassingQueue<E> queue, Consumer<E> c, WaitStrategy wait, ExitCondition exit) {
    if (null == c)
      throw new IllegalArgumentException("c is null");
    if (null == wait)
      throw new IllegalArgumentException("wait is null");
    if (null == exit)
      throw new IllegalArgumentException("exit condition is null");

    int idleCounter = 0;
    while (exit.keepRunning()) {
      final E e = queue.relaxedPoll();
      if (e == null) {
        idleCounter = wait.idle(idleCounter);
        continue;
      }
      idleCounter = 0;
      c.accept(e);
    }
  }

  public static <E> void fill(
          MessagePassingQueue<E> q, Supplier<E> s, WaitStrategy wait, ExitCondition exit) {
    if (null == wait)
      throw new IllegalArgumentException("waiter is null");
    if (null == exit)
      throw new IllegalArgumentException("exit condition is null");

    int idleCounter = 0;
    while (exit.keepRunning()) {
      if (q.fill(s, PortableJvmInfo.RECOMENDED_OFFER_BATCH) == 0) {
        idleCounter = wait.idle(idleCounter);
        continue;
      }
      idleCounter = 0;
    }
  }

  public static <E> int fillBounded(MessagePassingQueue<E> q, Supplier<E> s) {
    return fillInBatchesToLimit(q, s, PortableJvmInfo.RECOMENDED_OFFER_BATCH, q.capacity());
  }

  public static <E> int fillInBatchesToLimit(
          MessagePassingQueue<E> q, Supplier<E> s, int batch, int limit) {
    long result =
            0; // result is a long because we want to have a safepoint check at regular intervals
    do {
      final int filled = q.fill(s, batch);
      if (filled == 0) {
        return (int) result;
      }
      result += filled;
    }
    while (result <= limit);
    return (int) result;
  }

  public static <E> int fillUnbounded(MessagePassingQueue<E> q, Supplier<E> s) {
    return fillInBatchesToLimit(q, s, PortableJvmInfo.RECOMENDED_OFFER_BATCH, 4096);
  }
}
