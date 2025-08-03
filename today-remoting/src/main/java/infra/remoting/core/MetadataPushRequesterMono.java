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
package infra.remoting.core;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import infra.lang.NonNull;
import infra.lang.Nullable;
import infra.remoting.Payload;
import infra.remoting.frame.MetadataPushFrameCodec;
import io.netty.buffer.ByteBuf;
import io.netty.util.IllegalReferenceCountException;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import static infra.remoting.core.PayloadValidationUtils.INVALID_PAYLOAD_ERROR_MESSAGE;
import static infra.remoting.core.PayloadValidationUtils.isValidMetadata;
import static infra.remoting.core.StateUtils.isSubscribedOrTerminated;
import static infra.remoting.core.StateUtils.lazyTerminate;
import static infra.remoting.core.StateUtils.markSubscribed;

final class MetadataPushRequesterMono extends Mono<Void> implements Scannable {

  volatile long state;
  static final AtomicLongFieldUpdater<MetadataPushRequesterMono> STATE =
          AtomicLongFieldUpdater.newUpdater(MetadataPushRequesterMono.class, "state");

  final Payload payload;

  private final ChannelSupport channel;

  MetadataPushRequesterMono(Payload payload, ChannelSupport channel) {
    this.payload = payload;
    this.channel = channel;
  }

  @Override
  public void subscribe(CoreSubscriber<? super Void> actual) {
    long previousState = markSubscribed(STATE, this);
    if (isSubscribedOrTerminated(previousState)) {
      Operators.error(
              actual, new IllegalStateException("MetadataPushMono allows only a single Subscriber"));
      return;
    }

    final Payload p = this.payload;
    final ByteBuf metadata;
    try {
      final boolean hasMetadata = p.hasMetadata();
      metadata = p.metadata();
      if (!hasMetadata) {
        lazyTerminate(STATE, this);
        p.release();
        Operators.error(
                actual,
                new IllegalArgumentException("Metadata push should have metadata field present"));
        return;
      }
      if (!isValidMetadata(channel.maxFrameLength, metadata)) {
        lazyTerminate(STATE, this);
        p.release();
        Operators.error(
                actual,
                new IllegalArgumentException(
                        String.format(INVALID_PAYLOAD_ERROR_MESSAGE, channel.maxFrameLength)));
        return;
      }
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);
      Operators.error(actual, e);
      return;
    }

    final ByteBuf metadataRetainedSlice;
    try {
      metadataRetainedSlice = metadata.retainedSlice();
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);
      Operators.error(actual, e);
      return;
    }

    try {
      p.release();
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);
      metadataRetainedSlice.release();
      Operators.error(actual, e);
      return;
    }

    final ByteBuf requestFrame =
            MetadataPushFrameCodec.encode(channel.allocator, metadataRetainedSlice);
    channel.connection.sendFrame(0, requestFrame);

    Operators.complete(actual);
  }

  @Override
  @Nullable
  public Void block(Duration m) {
    return block();
  }

  /**
   * This method is deliberately non-blocking regardless it is named as `.block`. The main intent to
   * keep this method along with the {@link #subscribe()} is to eliminate redundancy which comes
   * with a default block method implementation.
   */
  @Override
  @Nullable
  public Void block() {
    long previousState = markSubscribed(STATE, this);
    if (isSubscribedOrTerminated(previousState)) {
      throw new IllegalStateException("MetadataPushMono allows only a single Subscriber");
    }

    final Payload p = this.payload;
    final ByteBuf metadata;
    try {
      final boolean hasMetadata = p.hasMetadata();
      metadata = p.metadata();
      if (!hasMetadata) {
        lazyTerminate(STATE, this);
        p.release();
        throw new IllegalArgumentException("Metadata push should have metadata field present");
      }
      if (!isValidMetadata(channel.maxFrameLength, metadata)) {
        lazyTerminate(STATE, this);
        p.release();
        throw new IllegalArgumentException(
                String.format(INVALID_PAYLOAD_ERROR_MESSAGE, channel.maxFrameLength));
      }
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);
      throw e;
    }

    final ByteBuf metadataRetainedSlice;
    try {
      metadataRetainedSlice = metadata.retainedSlice();
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);
      throw e;
    }

    try {
      p.release();
    }
    catch (IllegalReferenceCountException e) {
      lazyTerminate(STATE, this);
      metadataRetainedSlice.release();
      throw e;
    }

    final ByteBuf requestFrame = MetadataPushFrameCodec.encode(channel.allocator, metadataRetainedSlice);
    channel.connection.sendFrame(0, requestFrame);

    return null;
  }

  @Nullable
  @Override
  public Object scanUnsafe(Attr key) {
    return null; // no particular key to be represented, still useful in hooks
  }

  @Override
  @NonNull
  public String stepName() {
    return "source(MetadataPushMono)";
  }
}
