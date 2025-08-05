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

package infra.remoting.examples.tcp.lease.simple;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.lease.Lease;
import infra.remoting.lease.LeaseSender;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class LeaseExample {

  private static final Logger logger = LoggerFactory.getLogger(LeaseExample.class);

  private static final String SERVER_TAG = "server";
  private static final String CLIENT_TAG = "client";

  public static void main(String[] args) {
    // Queue for incoming messages represented as Flux
    // Imagine that every fireAndForget that is pushed is processed by a worker

    int queueCapacity = 50;
    BlockingQueue<String> messagesQueue = new ArrayBlockingQueue<>(queueCapacity);

    // emulating a worker that process data from the queue
    Thread workerThread =
            new Thread(
                    () -> {
                      try {
                        while (!Thread.currentThread().isInterrupted()) {
                          String message = messagesQueue.take();
                          logger.info("Process message {}", message);
                          Thread.sleep(500); // emulating processing
                        }
                      }
                      catch (InterruptedException e) {
                        throw new RuntimeException(e);
                      }
                    });

    workerThread.start();

    CloseableChannel server = RemotingServer.create((setup, sendingSocket) ->
                    Mono.just(new Channel() {
                      @Override
                      public Mono<Void> fireAndForget(Payload payload) {
                        // add element. if overflows errors and terminates execution
                        // specifically to show that lease can limit rate of fnf requests in
                        // that example
                        try {
                          if (!messagesQueue.offer(payload.getDataUtf8())) {
                            logger.error("Queue has been overflowed. Terminating execution");
                            sendingSocket.dispose();
                            workerThread.interrupt();
                          }
                        }
                        finally {
                          payload.release();
                        }
                        return Mono.empty();
                      }
                    }))
            .lease(leases -> leases.sender(new LeaseCalculator(SERVER_TAG, messagesQueue)))
            .bindNow(TcpServerTransport.create("localhost", 7000));

    Channel clientChannel =
            ChannelConnector.create()
                    .lease((config) -> config.maxPendingRequests(1))
                    .connect(TcpClientTransport.create(server.address()))
                    .block();

    Objects.requireNonNull(clientChannel);

    // generate stream of fnfs
    Flux.generate(
                    () -> 0L,
                    (state, sink) -> {
                      sink.next(state);
                      return state + 1;
                    })
            // here we wait for the first lease for the responder side and start execution
            // on if there is allowance
            .concatMap(
                    tick -> {
                      logger.info("Requesting FireAndForget({})", tick);
                      return clientChannel.fireAndForget(ByteBufPayload.create("" + tick));
                    })
            .blockLast();

    clientChannel.onClose().block();
    server.dispose();
  }

  /**
   * This is a class responsible for making decision on whether Responder is ready to receive new
   * FireAndForget or not base in the number of messages enqueued. <br>
   * In the nutshell this is responder-side rate-limiter logic which is created for every new
   * connection.<br>
   * In real-world projects this class has to issue leases based on real metrics
   */
  private static class LeaseCalculator implements LeaseSender {
    final String tag;
    final BlockingQueue<?> queue;

    public LeaseCalculator(String tag, BlockingQueue<?> queue) {
      this.tag = tag;
      this.queue = queue;
    }

    @Override
    public Flux<Lease> send() {
      Duration ttlDuration = Duration.ofSeconds(5);
      // The interval function is used only for the demo purpose and should not be
      // considered as the way to issue leases.
      // For advanced RateLimiting with Leasing
      // consider adopting https://github.com/Netflix/concurrency-limits#server-limiter
      return Flux.interval(Duration.ZERO, ttlDuration.dividedBy(2))
              .handle(
                      (__, sink) -> {
                        // put queue.remainingCapacity() + 1 here if you want to observe that app is
                        // terminated  because of the queue overflowing
                        int requests = queue.remainingCapacity();

                        // reissue new lease only if queue has remaining capacity to
                        // accept more requests
                        if (requests > 0) {
                          sink.next(Lease.create(ttlDuration, requests));
                        }
                      });
    }
  }
}
