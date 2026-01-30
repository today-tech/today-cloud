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

package infra.remoting.examples.tcp.lease.advanced.multiclient;

import com.netflix.concurrency.limits.limit.VegasLimit;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.ChannelAcceptor;
import infra.remoting.core.RemotingServer;
import infra.remoting.examples.tcp.lease.advanced.common.LeaseManager;
import infra.remoting.examples.tcp.lease.advanced.common.LimitBasedLeaseSender;
import infra.remoting.examples.tcp.lease.advanced.controller.TasksHandlingChannel;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class RespondingServer {

  private static final Logger logger = LoggerFactory.getLogger(RespondingServer.class);

  public static final int TASK_PROCESSING_TIME = 500;
  public static final int CONCURRENT_WORKERS_COUNT = 1;
  public static final int QUEUE_CAPACITY = 50;

  public static void main(String[] args) {
    // Queue for incoming messages represented as Flux
    // Imagine that every fireAndForget that is pushed is processed by a worker
    BlockingQueue<Runnable> tasksQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(1, CONCURRENT_WORKERS_COUNT, 1, TimeUnit.MINUTES, tasksQueue);

    Scheduler workScheduler = Schedulers.fromExecutorService(threadPoolExecutor);

    LeaseManager leaseManager = new LeaseManager(CONCURRENT_WORKERS_COUNT, TASK_PROCESSING_TIME);

    Disposable.Composite disposable = Disposables.composite();
    CloseableChannel server =
            RemotingServer.create(
                            ChannelAcceptor.with(
                                    new TasksHandlingChannel(disposable, workScheduler, TASK_PROCESSING_TIME)))
                    .lease(
                            (config) ->
                                    config.sender(
                                            new LimitBasedLeaseSender(
                                                    UUID.randomUUID().toString(),
                                                    leaseManager,
                                                    VegasLimit.newBuilder()
                                                            .initialLimit(CONCURRENT_WORKERS_COUNT)
                                                            .maxConcurrency(QUEUE_CAPACITY)
                                                            .build())))
                    .bindNow(TcpServerTransport.create("localhost", 7000));

    disposable.add(server);

    logger.info("Server started on port {}", server.address().getPort());
    server.onClose().block();
  }
}
