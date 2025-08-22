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

import infra.remoting.Channel;
import infra.remoting.Payload;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class TasksHandlingChannel implements Channel {

  private static final Logger logger = LoggerFactory.getLogger(TasksHandlingChannel.class);

  final Disposable terminatable;
  final Scheduler workScheduler;
  final int processingTime;

  public TasksHandlingChannel(Disposable terminatable, Scheduler scheduler, int processingTime) {
    this.terminatable = terminatable;
    this.workScheduler = scheduler;
    this.processingTime = processingTime;
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {

    // specifically to show that lease can limit rate of fnf requests in
    // that example
    String message = payload.getDataUtf8();
    payload.release();

    return Mono.<Void>fromRunnable(new Task(message, processingTime))
            // schedule task on specific, limited in size scheduler
            .subscribeOn(workScheduler)
            // if errors - terminates server
            .doOnError(
                    t -> {
                      logger.error("Queue has been overflowed. Terminating server");
                      terminatable.dispose();
                      System.exit(9);
                    });
  }
}
