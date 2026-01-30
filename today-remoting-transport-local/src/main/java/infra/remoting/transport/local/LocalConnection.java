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

package infra.remoting.transport.local;

import org.reactivestreams.Subscription;

import java.net.SocketAddress;
import java.util.Objects;

import infra.remoting.Connection;
import infra.remoting.ProtocolErrorException;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.internal.UnboundedProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

/** An implementation of {@link Connection} that connects inside the same JVM. */
final class LocalConnection implements Connection {

  private final LocalSocketAddress address;
  private final ByteBufAllocator allocator;
  private final UnboundedProcessor in;

  private final Mono<Void> onClose;

  private final UnboundedProcessor out;

  /**
   * Creates a new instance.
   *
   * @param name the name assigned to this local connection
   * @param in the inbound {@link ByteBuf}s
   * @param out the outbound {@link ByteBuf}s
   * @param onClose the closing notifier
   * @throws NullPointerException if {@code in}, {@code out}, or {@code onClose} are {@code null}
   */
  LocalConnection(String name, ByteBufAllocator allocator,
          UnboundedProcessor in, UnboundedProcessor out, Mono<Void> onClose) {
    this.address = new LocalSocketAddress(name);
    this.allocator = Objects.requireNonNull(allocator, "allocator is required");
    this.in = Objects.requireNonNull(in, "in is required");
    this.out = Objects.requireNonNull(out, "out is required");
    this.onClose = Objects.requireNonNull(onClose, "onClose is required");
  }

  @Override
  public void dispose() {
    out.onComplete();
  }

  @Override
  public boolean isDisposed() {
    return out.isDisposed();
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }

  @Override
  public Flux<ByteBuf> receive() {
    return in.transform(Operators.<ByteBuf, ByteBuf>lift(
            (__, actual) -> new ByteBufReleaserOperator(actual, this)));
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    if (streamId == 0) {
      out.tryEmitPrioritized(frame);
    }
    else {
      out.tryEmitNormal(frame);
    }
  }

  @Override
  public void sendErrorAndClose(ProtocolErrorException e) {
    final ByteBuf errorFrame = ErrorFrameCodec.encode(allocator, 0, e);
    out.tryEmitFinal(errorFrame);
  }

  @Override
  public ByteBufAllocator alloc() {
    return allocator;
  }

  @Override
  public SocketAddress remoteAddress() {
    return address;
  }

  @Override
  public String toString() {
    return "LocalConnection{" + "address=" + address + "hash=" + hashCode() + '}';
  }

  static class ByteBufReleaserOperator
          implements CoreSubscriber<ByteBuf>, Subscription, Fuseable.QueueSubscription<ByteBuf> {

    final CoreSubscriber<? super ByteBuf> actual;
    final LocalConnection parent;

    Subscription s;

    public ByteBufReleaserOperator(
            CoreSubscriber<? super ByteBuf> actual, LocalConnection parent) {
      this.actual = actual;
      this.parent = parent;
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

    @Override
    public void onError(Throwable t) {
      parent.out.onError(t);
      actual.onError(t);
    }

    @Override
    public void onComplete() {
      parent.out.onComplete();
      actual.onComplete();
    }

    @Override
    public void request(long n) {
      s.request(n);
    }

    @Override
    public void cancel() {
      s.cancel();
      parent.out.onComplete();
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
  }
}
