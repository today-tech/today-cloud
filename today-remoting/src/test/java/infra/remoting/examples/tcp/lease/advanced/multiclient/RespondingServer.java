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
