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
package infra.remoting.examples.tcp.fnf;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

/**
 * An example of long-running tasks processing (a.k.a Kafka style) where a client submits tasks over
 * request `FireAndForget` and then receives results over the same method but on it is own side.
 *
 * <p>This example shows a case when the client may disappear, however, another a client can connect
 * again and receive undelivered completed tasks remaining for the previous one.
 */
public class TaskProcessingWithServerSideNotificationsExample {

  public static void main(String[] args) throws InterruptedException {
    Sinks.Many<Task> tasksProcessor =
            Sinks.many().unicast().onBackpressureBuffer(Queues.<Task>unboundedMultiproducer().get());
    ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap = new ConcurrentHashMap<>();
    ConcurrentMap<String, Channel> idToChannelMap = new ConcurrentHashMap<>();
    BackgroundWorker backgroundWorker =
            new BackgroundWorker(tasksProcessor.asFlux(), idToCompletedTasksMap, idToChannelMap);

    RemotingServer.create(new TasksAcceptor(tasksProcessor, idToCompletedTasksMap, idToChannelMap))
            .bindNow(TcpServerTransport.create(9991));

    Logger logger = LoggerFactory.getLogger("Channel.Client.ID[Test]");

    Mono<Channel> channelMono =
            ChannelConnector.create()
                    .setupPayload(DefaultPayload.create("Test"))
                    .acceptor(
                            ChannelAcceptor.forFireAndForget(
                                    p -> {
                                      logger.info("Received Processed Task[{}]", p.getDataUtf8());
                                      p.release();
                                      return Mono.empty();
                                    }))
                    .connect(TcpClientTransport.create(9991));

    Channel channelRequester1 = channelMono.block();

    for (int i = 0; i < 10; i++) {
      channelRequester1.fireAndForget(DefaultPayload.create("task" + i)).block();
    }

    Thread.sleep(4000);

    channelRequester1.dispose();
    logger.info("Disposed");

    Thread.sleep(4000);

    Channel requester2 = channelMono.block();

    logger.info("Reconnected");

    Thread.sleep(10000);
  }

  static class BackgroundWorker extends BaseSubscriber<Task> {
    final ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap;
    final ConcurrentMap<String, Channel> idToChannelMap;

    BackgroundWorker(
            Flux<Task> taskProducer,
            ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap,
            ConcurrentMap<String, Channel> idToChannelMap) {

      this.idToCompletedTasksMap = idToCompletedTasksMap;
      this.idToChannelMap = idToChannelMap;

      // mimic a long running task processing
      taskProducer
              .concatMap(
                      t ->
                              Mono.delay(Duration.ofMillis(ThreadLocalRandom.current().nextInt(200, 2000)))
                                      .thenReturn(t))
              .subscribe(this);
    }

    @Override
    protected void hookOnNext(Task task) {
      BlockingQueue<Task> completedTasksQueue =
              idToCompletedTasksMap.computeIfAbsent(task.id, __ -> new LinkedBlockingQueue<>());

      completedTasksQueue.offer(task);
      Channel channel = idToChannelMap.get(task.id);
      if (channel != null) {
        channel
                .fireAndForget(DefaultPayload.create(task.content))
                .subscribe(null, e -> { }, () -> completedTasksQueue.remove(task));
      }
    }
  }

  static class TasksAcceptor implements ChannelAcceptor {

    static final Logger logger = LoggerFactory.getLogger(TasksAcceptor.class);

    final Sinks.Many<Task> tasksToProcess;
    final ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap;
    final ConcurrentMap<String, Channel> idToChannelMap;

    TasksAcceptor(
            Sinks.Many<Task> tasksToProcess,
            ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap,
            ConcurrentMap<String, Channel> idToChannelMap) {
      this.tasksToProcess = tasksToProcess;
      this.idToCompletedTasksMap = idToCompletedTasksMap;
      this.idToChannelMap = idToChannelMap;
    }

    @Override
    public Mono<Channel> accept(ConnectionSetupPayload setup, Channel channel) {
      String id = setup.getDataUtf8();
      logger.info("Accepting a new client connection with ID {}", id);
      // sendingChannel represents here an Channel requester to a remote peer

      if (this.idToChannelMap.compute(
              id, (__, old) -> old == null || old.isDisposed() ? channel : old)
              == channel) {
        return Mono.<Channel>just(
                        new ChannelTaskHandler(idToChannelMap, tasksToProcess, id, channel))
                .doOnSuccess(__ -> checkTasksToDeliver(channel, id));
      }

      return Mono.error(
              new IllegalStateException("There is already a client connected with the same ID"));
    }

    private void checkTasksToDeliver(Channel channel, String id) {
      logger.info("Accepted a new client connection with ID {}. Checking for remaining tasks", id);
      BlockingQueue<Task> tasksToDeliver = this.idToCompletedTasksMap.get(id);

      if (tasksToDeliver == null || tasksToDeliver.isEmpty()) {
        // means nothing yet to send
        return;
      }

      logger.info("Found remaining tasks to deliver for client {}", id);

      for (; ; ) {
        Task task = tasksToDeliver.poll();

        if (task == null) {
          return;
        }

        channel
                .fireAndForget(DefaultPayload.create(task.content))
                .subscribe(
                        null,
                        e -> {
                          // offers back a task if it has not been delivered
                          tasksToDeliver.offer(task);
                        });
      }
    }

    private static class ChannelTaskHandler implements Channel {

      private final String id;
      private final Channel channel;
      private ConcurrentMap<String, Channel> idToChannelMap;
      private Sinks.Many<Task> tasksToProcess;

      public ChannelTaskHandler(
              ConcurrentMap<String, Channel> idToChannelMap,
              Sinks.Many<Task> tasksToProcess,
              String id,
              Channel sendingSocket) {
        this.id = id;
        this.channel = sendingSocket;
        this.idToChannelMap = idToChannelMap;
        this.tasksToProcess = tasksToProcess;
      }

      @Override
      public Mono<Void> fireAndForget(Payload payload) {
        logger.info("Received a Task[{}] from Client.ID[{}]", payload.getDataUtf8(), id);
        Sinks.EmitResult result = tasksToProcess.tryEmitNext(new Task(id, payload.getDataUtf8()));
        payload.release();
        return result.isFailure() ? Mono.error(new Sinks.EmissionException(result)) : Mono.empty();
      }

      @Override
      public void dispose() {
        idToChannelMap.remove(id, channel);
      }
    }
  }

  static class Task {
    final String id;
    final String content;

    Task(String id, String content) {
      this.id = id;
      this.content = content;
    }
  }
}
