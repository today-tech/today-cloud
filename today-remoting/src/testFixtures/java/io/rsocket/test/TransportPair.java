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

package io.rsocket.test;

import org.reactivestreams.Subscription;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.ResourceLeakDetector;
import io.rsocket.Closeable;
import io.rsocket.DuplexConnection;
import io.rsocket.Channel;
import io.rsocket.RSocketErrorException;
import io.rsocket.core.ChannelConnector;
import io.rsocket.core.RemotingServer;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.plugins.ConnectionInterceptor;
import io.rsocket.resume.InMemoryResumableFramesStore;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.ServerTransport;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.Fuseable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import static io.rsocket.test.TransportTest.logger;

public class TransportPair<T, S extends Closeable> implements Disposable {

  private static final String data = "hello world";
  private static final String metadata = "metadata";

  final boolean withResumability;
  private final boolean runClientWithAsyncInterceptors;
  private final boolean runServerWithAsyncInterceptors;

  final LeaksTrackingByteBufAllocator byteBufAllocator1 =
          LeaksTrackingByteBufAllocator.instrument(
                  ByteBufAllocator.DEFAULT, Duration.ofMinutes(1), "Client");
  final LeaksTrackingByteBufAllocator byteBufAllocator2 =
          LeaksTrackingByteBufAllocator.instrument(
                  ByteBufAllocator.DEFAULT, Duration.ofMinutes(1), "Server");

  final TestChannel responder;

  private final Channel client;

  private final S server;

  public TransportPair(Supplier<T> addressSupplier,
          TriFunction<T, S, ByteBufAllocator, ClientTransport> clientTransportSupplier,
          BiFunction<T, ByteBufAllocator, ServerTransport<S>> serverTransportSupplier) {
    this(addressSupplier, clientTransportSupplier, serverTransportSupplier, false);
  }

  public TransportPair(Supplier<T> addressSupplier,
          TriFunction<T, S, ByteBufAllocator, ClientTransport> clientTransportSupplier,
          BiFunction<T, ByteBufAllocator, ServerTransport<S>> serverTransportSupplier,
          boolean withRandomFragmentation) {
    this(addressSupplier,
            clientTransportSupplier,
            serverTransportSupplier,
            withRandomFragmentation,
            false);
  }

  public TransportPair(Supplier<T> addressSupplier,
          TriFunction<T, S, ByteBufAllocator, ClientTransport> clientTransportSupplier,
          BiFunction<T, ByteBufAllocator, ServerTransport<S>> serverTransportSupplier,
          boolean withRandomFragmentation,
          boolean withResumability) {
    Schedulers.onHandleError((t, e) -> e.printStackTrace());
    Schedulers.resetFactory();

    this.withResumability = withResumability;

    T address = addressSupplier.get();

    this.runClientWithAsyncInterceptors = ThreadLocalRandom.current().nextBoolean();
    this.runServerWithAsyncInterceptors = ThreadLocalRandom.current().nextBoolean();

    ByteBufAllocator allocatorToSupply1;
    ByteBufAllocator allocatorToSupply2;
    if (ResourceLeakDetector.getLevel() == ResourceLeakDetector.Level.ADVANCED
            || ResourceLeakDetector.getLevel() == ResourceLeakDetector.Level.PARANOID) {
      logger.info("Using LeakTrackingByteBufAllocator");
      allocatorToSupply1 = byteBufAllocator1;
      allocatorToSupply2 = byteBufAllocator2;
    }
    else {
      allocatorToSupply1 = ByteBufAllocator.DEFAULT;
      allocatorToSupply2 = ByteBufAllocator.DEFAULT;
    }
    responder = new TestChannel(TransportPair.data, metadata);
    final RemotingServer remotingServer = RemotingServer.create((setup, sendingSocket) -> Mono.just(responder))
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .interceptors(registry -> {
              if (runServerWithAsyncInterceptors && !withResumability) {
                logger.info("Perform Integration Test with Async Interceptors Enabled For Server");
                registry.forConnection((type, duplexConnection) -> new AsyncDuplexConnection(duplexConnection, "server"))
                        .forChannelAcceptor(delegate -> (connectionSetupPayload, sendingSocket) -> delegate.accept(connectionSetupPayload, sendingSocket)
                                .subscribeOn(Schedulers.parallel()));
              }

              if (withResumability) {
                registry.forConnection((type, duplexConnection) ->
                        type == ConnectionInterceptor.Type.SOURCE
                                ? new DisconnectingDuplexConnection(
                                "Server",
                                duplexConnection,
                                Duration.ofMillis(ThreadLocalRandom.current().nextInt(100, 1000)))
                                : duplexConnection);
              }
            });

    if (withResumability) {
      remotingServer.resume(new Resume()
              .storeFactory(token -> new InMemoryResumableFramesStore("server", token, Integer.MAX_VALUE)));
    }

    if (withRandomFragmentation) {
      remotingServer.fragment(ThreadLocalRandom.current().nextInt(256, 512));
    }

    server =
            remotingServer.bind(serverTransportSupplier.apply(address, allocatorToSupply2)).block();

    final ChannelConnector channelConnector =
            ChannelConnector.create()
                    .payloadDecoder(PayloadDecoder.ZERO_COPY)
                    .keepAlive(Duration.ofMillis(10), Duration.ofHours(1))
                    .interceptors(registry -> {
                      if (runClientWithAsyncInterceptors && !withResumability) {
                        logger.info("Perform Integration Test with Async Interceptors Enabled For Client");
                        registry.forConnection((type, duplexConnection) ->
                                        new AsyncDuplexConnection(duplexConnection, "client"))
                                .forChannelAcceptor(delegate -> (connectionSetupPayload, sendingSocket) ->
                                        delegate.accept(connectionSetupPayload, sendingSocket)
                                                .subscribeOn(Schedulers.parallel()));
                      }

                      if (withResumability) {
                        registry.forConnection((type, duplexConnection) ->
                                type == ConnectionInterceptor.Type.SOURCE
                                        ? new DisconnectingDuplexConnection(
                                        "Client",
                                        duplexConnection,
                                        Duration.ofMillis(
                                                ThreadLocalRandom.current().nextInt(10, 1500)))
                                        : duplexConnection);
                      }
                    });

    if (withResumability) {
      channelConnector.resume(
              new Resume().storeFactory(
                      token -> new InMemoryResumableFramesStore("client", token, Integer.MAX_VALUE)));
    }

    if (withRandomFragmentation) {
      channelConnector.fragment(ThreadLocalRandom.current().nextInt(256, 512));
    }

    client =
            channelConnector
                    .connect(clientTransportSupplier.apply(address, server, allocatorToSupply1))
                    .doOnError(Throwable::printStackTrace)
                    .block();
  }

  @Override
  public void dispose() {
    logger.info("terminating transport pair");
    client.dispose();
  }

  Channel getClient() {
    return client;
  }

  public String expectedPayloadData() {
    return data;
  }

  public String expectedPayloadMetadata() {
    return metadata;
  }

  public void awaitClosed(Duration timeout) {
    logger.info("awaiting termination of transport pair");
    logger.info(
            "wrappers combination: client{async="
                    + runClientWithAsyncInterceptors
                    + "; resume="
                    + withResumability
                    + "} server{async="
                    + runServerWithAsyncInterceptors
                    + "; resume="
                    + withResumability
                    + "}");
    client
            .onClose()
            .doOnSubscribe(s -> logger.info("Client termination stage=onSubscribe(" + s + ")"))
            .doOnEach(s -> logger.info("Client termination stage=" + s))
            .onErrorResume(t -> Mono.empty())
            .doOnTerminate(() -> logger.info("Client terminated. Terminating Server"))
            .then(Mono.fromRunnable(server::dispose))
            .then(server.onClose()
                    .doOnSubscribe(s -> logger.info("Server termination stage=onSubscribe(" + s + ")"))
                    .doOnEach(s -> logger.info("Server termination stage=" + s)))
            .onErrorResume(t -> Mono.empty())
            .block(timeout);

    logger.info("TransportPair has been terminated");
  }

  private static class AsyncDuplexConnection implements DuplexConnection {

    private final DuplexConnection duplexConnection;
    private String tag;
    private final ByteBufReleaserOperator bufReleaserOperator;

    public AsyncDuplexConnection(DuplexConnection duplexConnection, String tag) {
      this.duplexConnection = duplexConnection;
      this.tag = tag;
      this.bufReleaserOperator = new ByteBufReleaserOperator();
    }

    @Override
    public void sendFrame(int streamId, ByteBuf frame) {
      duplexConnection.sendFrame(streamId, frame);
    }

    @Override
    public void sendErrorAndClose(RSocketErrorException e) {
      duplexConnection.sendErrorAndClose(e);
    }

    @Override
    public Flux<ByteBuf> receive() {
      return duplexConnection
              .receive()
              .doOnTerminate(() -> logger.info("[" + this + "] Receive is done before PO"))
              .subscribeOn(Schedulers.boundedElastic())
              .doOnNext(ByteBuf::retain)
              .publishOn(Schedulers.boundedElastic(), Integer.MAX_VALUE)
              .doOnTerminate(() -> logger.info("[" + this + "] Receive is done after PO"))
              .doOnDiscard(ReferenceCounted.class, ReferenceCountUtil::safeRelease)
              .transform(Operators.<ByteBuf, ByteBuf>lift((__, actual) -> {
                bufReleaserOperator.actual = actual;
                return bufReleaserOperator;
              }));
    }

    @Override
    public ByteBufAllocator alloc() {
      return duplexConnection.alloc();
    }

    @Override
    public SocketAddress remoteAddress() {
      return duplexConnection.remoteAddress();
    }

    @Override
    public Mono<Void> onClose() {
      return Mono.whenDelayError(duplexConnection.onClose()
                      .doOnTerminate(() -> logger.info("[" + this + "] Source Connection is done")),
              bufReleaserOperator
                      .onClose()
                      .doOnTerminate(() -> logger.info("[" + this + "] BufferReleaser is done")));
    }

    @Override
    public void dispose() {
      duplexConnection.dispose();
    }

    @Override
    public String toString() {
      return "AsyncDuplexConnection{"
              + "duplexConnection="
              + duplexConnection
              + ", tag='"
              + tag
              + '\''
              + ", bufReleaserOperator="
              + bufReleaserOperator
              + '}';
    }
  }

  private static class DisconnectingDuplexConnection implements DuplexConnection {

    private final String tag;
    final DuplexConnection source;
    final Duration delay;
    final Swap disposables = Disposables.swap();

    DisconnectingDuplexConnection(String tag, DuplexConnection source, Duration delay) {
      this.tag = tag;
      this.source = source;
      this.delay = delay;
    }

    @Override
    public void dispose() {
      disposables.dispose();
      source.dispose();
    }

    @Override
    public Mono<Void> onClose() {
      return source
              .onClose()
              .doOnTerminate(() -> logger.info("[" + this + "] Source Connection is done"));
    }

    @Override
    public void sendFrame(int streamId, ByteBuf frame) {
      source.sendFrame(streamId, frame);
    }

    @Override
    public void sendErrorAndClose(RSocketErrorException errorException) {
      source.sendErrorAndClose(errorException);
    }

    boolean receivedFirst;

    @Override
    public Flux<ByteBuf> receive() {
      return source
              .receive()
              .doOnSubscribe(__ -> logger.warn("Tag {}. Subscribing Connection[{}]", tag, source.hashCode()))
              .doOnNext(bb -> {
                if (!receivedFirst) {
                  receivedFirst = true;
                  disposables.replace(
                          Mono.delay(delay)
                                  .takeUntilOther(source.onClose())
                                  .subscribe(__ -> {
                                    logger.warn("Tag {}. Disposing Connection[{}]", tag, source.hashCode());
                                    source.dispose();
                                  }));
                }
              });
    }

    @Override
    public ByteBufAllocator alloc() {
      return source.alloc();
    }

    @Override
    public SocketAddress remoteAddress() {
      return source.remoteAddress();
    }

    @Override
    public String toString() {
      return "DisconnectingDuplexConnection{"
              + "tag='"
              + tag
              + '\''
              + ", source="
              + source
              + ", disposables="
              + disposables
              + '}';
    }
  }

  private static class ByteBufReleaserOperator
          implements CoreSubscriber<ByteBuf>, Subscription, Fuseable.QueueSubscription<ByteBuf> {

    CoreSubscriber<? super ByteBuf> actual;
    final Sinks.Empty<Void> closeableMonoSink;

    Subscription s;

    public ByteBufReleaserOperator() {
      this.closeableMonoSink = Sinks.unsafe().empty();
    }

    @Override
    public void onSubscribe(Subscription s) {
      if (Operators.validate(this.s, s)) {
        this.s = s;
        actual.onSubscribe(this);
      }
    }

    @Override
    public void onNext(ByteBuf buf) {
      try {
        actual.onNext(buf);
      }
      finally {
        buf.release();
      }
    }

    Mono<Void> onClose() {
      return closeableMonoSink.asMono();
    }

    @Override
    public void onError(Throwable t) {
      actual.onError(t);
      closeableMonoSink.tryEmitError(t);
    }

    @Override
    public void onComplete() {
      actual.onComplete();
      closeableMonoSink.tryEmitEmpty();
    }

    @Override
    public void request(long n) {
      s.request(n);
    }

    @Override
    public void cancel() {
      s.cancel();
      closeableMonoSink.tryEmitEmpty();
    }

    @Override
    public int requestFusion(int requestedMode) {
      return Fuseable.NONE;
    }

    @Override
    public ByteBuf poll() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public int size() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public boolean isEmpty() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public String toString() {
      return "ByteBufReleaserOperator{"
              + "isActualPresent="
              + (actual != null)
              + ", "
              + "isSubscriptionPresent="
              + (s != null)
              + '}';
    }
  }
}
