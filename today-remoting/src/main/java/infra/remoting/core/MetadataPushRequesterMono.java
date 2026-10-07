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
package infra.remoting.core;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

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
  public String stepName() {
    return "source(MetadataPushMono)";
  }
}
