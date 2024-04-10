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

package cn.taketoday.cloud;

import java.util.concurrent.ExecutionException;

import cn.taketoday.util.concurrent.Future;

/**
 * @author TODAY 2021/7/9 21:55
 */
public class SimpleRemoteExceptionHandler implements RemoteExceptionHandler {

  @Override
  public Object handle(Future<Object> response) throws Throwable {
    throw exceptionNow(response);
  }

  Throwable exceptionNow(Future<Object> response) {
    if (!response.isDone())
      throw new IllegalStateException("Task has not completed");
    if (response.isCancelled())
      throw new IllegalStateException("Task was cancelled");
    boolean interrupted = false;
    try {
      while (true) {
        try {
          response.get();
          throw new IllegalStateException("Task completed with a result");
        }
        catch (InterruptedException e) {
          interrupted = true;
        }
        catch (ExecutionException e) {
          return e.getCause();
        }
      }
    }
    finally {
      if (interrupted)
        Thread.currentThread().interrupt();
    }
  }

}
