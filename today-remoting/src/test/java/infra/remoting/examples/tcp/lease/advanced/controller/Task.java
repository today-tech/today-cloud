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

package infra.remoting.examples.tcp.lease.advanced.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// emulating a worker that process data from the queue
public class Task implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(Task.class);

  final String message;
  final int processingTime;

  Task(String message, int processingTime) {
    this.message = message;
    this.processingTime = processingTime;
  }

  @Override
  public void run() {
    logger.info("Processing Task[{}]", message);
    try {
      Thread.sleep(processingTime); // emulating processing
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
