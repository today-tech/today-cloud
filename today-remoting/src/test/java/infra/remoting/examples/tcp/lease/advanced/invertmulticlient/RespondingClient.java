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

package infra.remoting.examples.tcp.lease.advanced.invertmulticlient;

import com.netflix.concurrency.limits.limit.VegasLimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.core.ChannelConnector;
import infra.remoting.examples.tcp.lease.advanced.common.LeaseManager;
import infra.remoting.examples.tcp.lease.advanced.common.LimitBasedLeaseSender;
import infra.remoting.examples.tcp.lease.advanced.controller.TasksHandlingChannel;
import infra.remoting.transport.netty.client.TcpClientTransport;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class RespondingClient {
  private static final Logger logger = LoggerFactory.getLogger(RespondingClient.class);

  public static final int PROCESSING_TASK_TIME = 500;
  public static final int CONCURRENT_WORKERS_COUNT = 1;
  public static final int QUEUE_CAPACITY = 50;

  public static void main(String[] args) {
    // Queue for incoming messages represented as Flux
    // Imagine that every fireAndForget that is pushed is processed by a worker
    BlockingQueue<Runnable> tasksQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(1, CONCURRENT_WORKERS_COUNT, 1, TimeUnit.MINUTES, tasksQueue);

    Scheduler workScheduler = Schedulers.fromExecutorService(threadPoolExecutor);

    LeaseManager periodicLeaseSender =
            new LeaseManager(CONCURRENT_WORKERS_COUNT, PROCESSING_TASK_TIME);

    Disposable.Composite disposable = Disposables.composite();
    Channel clientChannel =
            ChannelConnector.create()
                    .acceptor(
                            ChannelAcceptor.with(
                                    new TasksHandlingChannel(disposable, workScheduler, PROCESSING_TASK_TIME)))
                    .lease(
                            (config) ->
                                    config.sender(
                                            new LimitBasedLeaseSender(
                                                    UUID.randomUUID().toString(),
                                                    periodicLeaseSender,
                                                    VegasLimit.newBuilder()
                                                            .initialLimit(CONCURRENT_WORKERS_COUNT)
                                                            .maxConcurrency(QUEUE_CAPACITY)
                                                            .build())))
                    .connect(TcpClientTransport.create("localhost", 7000))
                    .block();

    Objects.requireNonNull(clientChannel);
    disposable.add(clientChannel);
    clientChannel.onClose().block();
  }
}
