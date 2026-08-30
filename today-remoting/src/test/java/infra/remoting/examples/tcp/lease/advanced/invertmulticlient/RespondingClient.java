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
