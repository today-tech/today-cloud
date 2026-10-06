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

package infra.cloud.service;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CancellationException;

import infra.util.Assert;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Shared result factories and Reactor bridges. Interfaces are Reactor-independent;
 * these implementations use Reactor internally. Value and completion futures are
 * stable handles; cancelling either cancels the associated work.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/6
 */
public final class InvocationResults {

  private InvocationResults() {
  }

  public static SingleInvocationResult single(Publisher<Object> source) {
    return new Single(source);
  }

  public static StreamingInvocationResult stream(InvocationType type, Publisher<Object> source) {
    Assert.isTrue(type == InvocationType.RESPONSE_STREAMING || type == InvocationType.DUPLEX_STREAMING,
            "Streaming invocation type required");
    return new Stream(type, source);
  }

  public static InvocationResult completion(Publisher<?> source) {
    return new Completion(source);
  }

  public static SingleInvocationResult success(@Nullable Object value) {
    return single(Mono.justOrEmpty(value));
  }

  public static SingleInvocationResult failure(Throwable error) {
    return single(Mono.error(error));
  }

  /** Adapt a result without interpreting data access as a fresh invocation. */
  public static Publisher<Object> publisher(InvocationResult result) {
    if (result instanceof SingleInvocationResult single) {
      return Mono.defer(() -> {
        result.start();
        return Mono.fromCompletionStage(single.value().toCompletableFuture()).doOnCancel(result::cancel);
      });
    }
    if (result instanceof StreamingInvocationResult stream) {
      return stream.values();
    }
    return Mono.defer(() -> {
      result.start();
      return Mono.fromCompletionStage(result.completion().toCompletableFuture()).then(Mono.<Object>empty())
              .doOnCancel(result::cancel);
    });
  }

  private abstract static class Base implements InvocationResult {

    final Promise<Void> done = Future.forPromise(Runnable::run);

    final AtomicReference<Subscription> subscription = new AtomicReference<>();

    final AtomicBoolean started = new AtomicBoolean();

    final InvocationType type;

    Base(InvocationType type) {
      this.type = type;
      done.onCompleted(future -> {
        if (future.isCancelled()) {
          cancel();
        }
      });
    }

    public InvocationType getType() {
      return type;
    }

    public Future<Void> completion() {
      return done;
    }

    void subscribe(Subscription next) {
      if (!subscription.compareAndSet(null, next) || done.isCancelled()) {
        next.cancel();
      }
    }

    public boolean cancel() {
      boolean cancelled = done.cancel();
      if (done.isCancelled()) {
        Subscription current = subscription.getAndSet(null);
        if (current != null) {
          current.cancel();
        }
      }
      return cancelled;
    }
  }

  private static final class Single extends Base implements SingleInvocationResult {

    final Publisher<Object> source;

    final Promise<Object> value = Future.forPromise(Runnable::run);

    Single(Publisher<Object> source) {
      super(InvocationType.REQUEST_RESPONSE);
      Assert.notNull(source, "source is required");
      this.source = source;
      value.onCompleted(future -> {
        if (future.isCancelled()) {
          cancel();
        }
      });
    }

    public Future<Object> value() {
      return value;
    }

    public boolean cancel() {
      boolean cancelled = super.cancel();
      if (done.isCancelled()) {
        value.cancel();
      }
      return cancelled;
    }

    public synchronized void start() {
      if (!started.compareAndSet(false, true) || done.isDone()) {
        return;
      }
      Mono.from(source).doOnSubscribe(this::subscribe).subscribe(
              result -> {
                value.trySuccess(result);
                done.trySuccess(null);
              },
              error -> {
                value.tryFailure(error);
                done.tryFailure(error);
              },
              () -> {
                value.trySuccess(null);
                done.trySuccess(null);
              });
    }
  }

  private static final class Completion extends Base {

    final Publisher<?> source;

    Completion(Publisher<?> source) {
      super(InvocationType.FIRE_AND_FORGET);
      this.source = source;
    }

    public void start() {
      if (!started.compareAndSet(false, true) || done.isDone()) {
        return;
      }
      Flux.from(source).then().doOnSubscribe(this::subscribe).subscribe(
              ignored -> { }, done::tryFailure, () -> done.trySuccess(null));
    }
  }

  private static final class Stream extends Base implements StreamingInvocationResult {

    final Publisher<Object> values;

    Stream(InvocationType type, Publisher<Object> source) {
      super(type);
      Assert.notNull(source, "source is required");
      values = Flux.defer(() -> {
        if (!started.compareAndSet(false, true)) {
          return Flux.error(new IllegalStateException("Only one stream subscription allowed"));
        }
        if (done.isCancelled()) {
          return Flux.error(new CancellationException());
        }
        return Flux.from(source).doOnSubscribe(this::subscribe)
                .doOnComplete(() -> done.trySuccess(null)).doOnError(done::tryFailure).doOnCancel(this::cancel);
      });
    }

    public Publisher<Object> values() {
      return values;
    }

    public void start() {
      // Data subscription owns stream demand.
    }
  }
}
