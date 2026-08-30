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
