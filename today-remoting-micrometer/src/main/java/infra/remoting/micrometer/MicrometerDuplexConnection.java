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

package infra.remoting.micrometer;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.DuplexConnection;
import infra.remoting.ProtocolErrorException;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameType;
import infra.remoting.plugins.ConnectionDecorator.Type;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static infra.remoting.frame.FrameType.CANCEL;
import static infra.remoting.frame.FrameType.COMPLETE;
import static infra.remoting.frame.FrameType.ERROR;
import static infra.remoting.frame.FrameType.EXT;
import static infra.remoting.frame.FrameType.KEEPALIVE;
import static infra.remoting.frame.FrameType.LEASE;
import static infra.remoting.frame.FrameType.METADATA_PUSH;
import static infra.remoting.frame.FrameType.NEXT;
import static infra.remoting.frame.FrameType.NEXT_COMPLETE;
import static infra.remoting.frame.FrameType.PAYLOAD;
import static infra.remoting.frame.FrameType.REQUEST_CHANNEL;
import static infra.remoting.frame.FrameType.REQUEST_FNF;
import static infra.remoting.frame.FrameType.REQUEST_N;
import static infra.remoting.frame.FrameType.REQUEST_RESPONSE;
import static infra.remoting.frame.FrameType.REQUEST_STREAM;
import static infra.remoting.frame.FrameType.RESUME;
import static infra.remoting.frame.FrameType.RESUME_OK;
import static infra.remoting.frame.FrameType.SETUP;

/**
 * An implementation of {@link DuplexConnection} that intercepts frames and gathers Micrometer
 * metrics about them.
 *
 * <p>The metric is called {@code infra.remoting.frame} and is tagged with {@code connection.type} ({@link
 * Type}), {@code frame.type} ({@link FrameType}), and any additional configured tags. {@code
 * infra.remoting.duplex.connection.close} and {@code infra.remoting.duplex.connection.dispose} metrics, tagged
 * with {@code connection.type} ({@link Type}) and any additional configured tags are also
 * collected.
 *
 * @see <a href="https://micrometer.io">Micrometer</a>
 */
final class MicrometerDuplexConnection implements DuplexConnection {

  private final Counter close;

  private final DuplexConnection delegate;

  private final Counter dispose;

  private final FrameCounters frameCounters;

  /**
   * Creates a new {@link DuplexConnection}.
   *
   * @param connectionType the type of connection being monitored
   * @param delegate the {@link DuplexConnection} to delegate to
   * @param meterRegistry the {@link MeterRegistry} to use
   * @param tags additional tags to attach to {@link Meter}s
   * @throws NullPointerException if {@code connectionType}, {@code delegate}, or {@code
   * meterRegistry} is {@code null}
   */
  MicrometerDuplexConnection(Type connectionType, DuplexConnection delegate, MeterRegistry meterRegistry, Tag... tags) {
    Objects.requireNonNull(connectionType, "connectionType is required");
    this.delegate = Objects.requireNonNull(delegate, "delegate is required");
    Objects.requireNonNull(meterRegistry, "meterRegistry is required");

    this.close = meterRegistry.counter("infra.remoting.duplex.connection.close",
            Tags.of(tags).and("connection.type", connectionType.name()));

    this.dispose = meterRegistry.counter("infra.remoting.duplex.connection.dispose",
            Tags.of(tags).and("connection.type", connectionType.name()));

    this.frameCounters = new FrameCounters(connectionType, meterRegistry, tags);
  }

  @Override
  public ByteBufAllocator alloc() {
    return delegate.alloc();
  }

  @Override
  public SocketAddress remoteAddress() {
    return delegate.remoteAddress();
  }

  @Override
  public void dispose() {
    delegate.dispose();
    dispose.increment();
  }

  @Override
  public Mono<Void> onClose() {
    return delegate.onClose().doAfterTerminate(close::increment);
  }

  @Override
  public Flux<ByteBuf> receive() {
    return delegate.receive().doOnNext(frameCounters);
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    frameCounters.accept(frame);
    delegate.sendFrame(streamId, frame);
  }

  @Override
  public void sendErrorAndClose(ProtocolErrorException e) {
    delegate.sendErrorAndClose(e);
  }

  private static final class FrameCounters implements Consumer<ByteBuf> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Counter cancel;

    private final Counter complete;

    private final Counter error;

    private final Counter extension;

    private final Counter keepalive;

    private final Counter lease;

    private final Counter metadataPush;

    private final Counter next;

    private final Counter nextComplete;

    private final Counter payload;

    private final Counter requestChannel;

    private final Counter requestFireAndForget;

    private final Counter requestN;

    private final Counter requestResponse;

    private final Counter requestStream;

    private final Counter resume;

    private final Counter resumeOk;

    private final Counter setup;

    private final Counter unknown;

    private FrameCounters(Type connectionType, MeterRegistry meterRegistry, Tag... tags) {
      this.cancel = counter(connectionType, meterRegistry, CANCEL, tags);
      this.complete = counter(connectionType, meterRegistry, COMPLETE, tags);
      this.error = counter(connectionType, meterRegistry, ERROR, tags);
      this.extension = counter(connectionType, meterRegistry, EXT, tags);
      this.keepalive = counter(connectionType, meterRegistry, KEEPALIVE, tags);
      this.lease = counter(connectionType, meterRegistry, LEASE, tags);
      this.metadataPush = counter(connectionType, meterRegistry, METADATA_PUSH, tags);
      this.next = counter(connectionType, meterRegistry, NEXT, tags);
      this.nextComplete = counter(connectionType, meterRegistry, NEXT_COMPLETE, tags);
      this.payload = counter(connectionType, meterRegistry, PAYLOAD, tags);
      this.requestChannel = counter(connectionType, meterRegistry, REQUEST_CHANNEL, tags);
      this.requestFireAndForget = counter(connectionType, meterRegistry, REQUEST_FNF, tags);
      this.requestN = counter(connectionType, meterRegistry, REQUEST_N, tags);
      this.requestResponse = counter(connectionType, meterRegistry, REQUEST_RESPONSE, tags);
      this.requestStream = counter(connectionType, meterRegistry, REQUEST_STREAM, tags);
      this.resume = counter(connectionType, meterRegistry, RESUME, tags);
      this.resumeOk = counter(connectionType, meterRegistry, RESUME_OK, tags);
      this.setup = counter(connectionType, meterRegistry, SETUP, tags);
      this.unknown = counter(connectionType, meterRegistry, "UNKNOWN", tags);
    }

    private static Counter counter(
            Type connectionType, MeterRegistry meterRegistry, FrameType frameType, Tag... tags) {

      return counter(connectionType, meterRegistry, frameType.name(), tags);
    }

    private static Counter counter(
            Type connectionType, MeterRegistry meterRegistry, String frameType, Tag... tags) {

      return meterRegistry.counter(
              "infra.remoting.frame",
              Tags.of(tags).and("connection.type", connectionType.name()).and("frame.type", frameType));
    }

    @Override
    public void accept(ByteBuf frame) {
      FrameType frameType = FrameHeaderCodec.frameType(frame);

      switch (frameType) {
        case SETUP:
          this.setup.increment();
          break;
        case LEASE:
          this.lease.increment();
          break;
        case KEEPALIVE:
          this.keepalive.increment();
          break;
        case REQUEST_RESPONSE:
          this.requestResponse.increment();
          break;
        case REQUEST_FNF:
          this.requestFireAndForget.increment();
          break;
        case REQUEST_STREAM:
          this.requestStream.increment();
          break;
        case REQUEST_CHANNEL:
          this.requestChannel.increment();
          break;
        case REQUEST_N:
          this.requestN.increment();
          break;
        case CANCEL:
          this.cancel.increment();
          break;
        case PAYLOAD:
          this.payload.increment();
          break;
        case ERROR:
          this.error.increment();
          break;
        case METADATA_PUSH:
          this.metadataPush.increment();
          break;
        case RESUME:
          this.resume.increment();
          break;
        case RESUME_OK:
          this.resumeOk.increment();
          break;
        case NEXT:
          this.next.increment();
          break;
        case COMPLETE:
          this.complete.increment();
          break;
        case NEXT_COMPLETE:
          this.nextComplete.increment();
          break;
        case EXT:
          this.extension.increment();
          break;
        default:
          this.logger.debug("Skipping count of unknown frame type: {}", frameType);
          this.unknown.increment();
      }
    }
  }
}
