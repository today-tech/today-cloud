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

package io.rsocket.util;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.function.Supplier;

import infra.util.concurrent.Future;
import infra.util.concurrent.FutureListener;
import io.rsocket.AbortedException;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/3 11:34
 */
public abstract class FutureMono<T> extends Mono<T> {

  /**
   * Convert a {@link Future} into {@link Mono}. {@link Mono#subscribe(Subscriber)}
   * will bridge to {@link Future#onCompleted(FutureListener)}.
   *
   * @param future the future to convert from
   * @param <F> the future type
   * @return A {@link Mono} forwarding {@link Future} success or failure
   */
  public static <T, F extends Future<T>> Mono<T> of(F future) {
    Objects.requireNonNull(future, "future");
    if (future.isDone()) {
      if (!future.isSuccess()) {
        return Mono.error(FutureSubscription.wrapError(future.getCause()));
      }
      return Mono.empty();
    }
    return new ImmediateFutureMono<>(future);
  }

  /**
   * Convert a supplied {@link Future} for each subscriber into {@link Mono}.
   * {@link Mono#subscribe(Subscriber)}
   * will bridge to {@link Future#onCompleted(FutureListener)}.
   *
   * @param deferredFuture the future to evaluate and convert from
   * @param <F> the future type
   * @return A {@link Mono} forwarding {@link Future} success or failure
   */
  public static <T, F extends Future<T>> Mono<T> deferFuture(Supplier<F> deferredFuture) {
    return new DeferredFutureMono<>(deferredFuture);
  }

  static final class ImmediateFutureMono<T, F extends Future<T>> extends FutureMono<T> {

    final F future;

    ImmediateFutureMono(F future) {
      this.future = Objects.requireNonNull(future, "future");
    }

    @Override
    public void subscribe(final CoreSubscriber<? super T> s) {
      doSubscribe(s, future);
    }
  }

  static final class DeferredFutureMono<T, F extends Future<T>> extends FutureMono<T> {

    final Supplier<F> deferredFuture;

    DeferredFutureMono(Supplier<F> deferredFuture) {
      this.deferredFuture = Objects.requireNonNull(deferredFuture, "deferredFuture");
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> s) {
      F f;
      try {
        f = deferredFuture.get();
      }
      catch (Throwable t) {
        Operators.error(s, t);
        return;
      }

      if (f == null) {
        Operators.error(s,
                Operators.onOperatorError(new NullPointerException(
                        "Deferred supplied null"), s.currentContext()));
        return;
      }

      doSubscribe(s, f);
    }
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  static <T, F extends Future<T>> void doSubscribe(CoreSubscriber<? super T> s, F future) {
    if (future.isDone()) {
      if (future.isSuccess()) {
        Operators.complete(s);
      }
      else {
        Operators.error(s, FutureSubscription.wrapError(future.getCause()));
      }
      return;
    }

    FutureSubscription<T, F> fs = new FutureSubscription<>(future, s);
    // propagate subscription before adding listener to avoid any race between finishing future and onSubscribe
    // is called
    s.onSubscribe(fs);

    // check if subscription was not cancelled immediately.
    if (fs.cancelled) {
      // if so do nothing anymore
      return;
    }

    // add listener to the future to propagate on complete when future is done
    // onCompleted likely to be thread safe method
    future.onCompleted(fs);

    // check once again if is cancelled to see if we need to removeListener in case onCompleted racing with
    // subscription.cancel (which should remove listener)
    if (fs.cancelled) {
      // Returned value is deliberately ignored
      future.cancel();
    }
  }

  static final class FutureSubscription<T, F extends Future<T>>
          implements FutureListener<F>, Subscription, Supplier<Context> {

    final CoreSubscriber<? super T> s;

    final F future;

    boolean cancelled;

    FutureSubscription(F future, CoreSubscriber<? super T> s) {
      this.s = s;
      this.future = future;
    }

    @Override
    public void request(long n) {
      //noop
    }

    @Override
    public Context get() {
      return s.currentContext();
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void cancel() {
      // cancel is not thread safe since we assume that removeListener is thread-safe. That said if we have
      // concurrent onCompleted and removeListener and if onCompleted is after removeListener, the other Thread
      // after execution onCompleted should see changes happened before removeListener. Thus, it should see
      // cancelled flag set to true and should cleanup added handler
      this.cancelled = true;
      future.cancel();
    }

    @Override
    public void operationComplete(F future) {
      if (cancelled) {
        // Returned value is deliberately ignored
        return;
      }
      if (future.isSuccess()) {
        T now = future.getNow();
        if (now != null) {
          s.onNext(now);
        }
        s.onComplete();
      }
      else {
        s.onError(wrapError(future.getCause()));
      }
    }

    private static Throwable wrapError(Throwable error) {
      if (error instanceof ClosedChannelException) {
        return new AbortedException(error);
      }
      else {
        return error;
      }
    }
  }
}
