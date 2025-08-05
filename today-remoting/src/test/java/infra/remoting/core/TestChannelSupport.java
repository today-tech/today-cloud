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

import org.assertj.core.api.Assertions;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import infra.remoting.Connection;
import infra.remoting.Payload;
import infra.remoting.Channel;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.plugins.RequestInterceptor;
import infra.remoting.test.util.TestConnection;
import infra.remoting.util.ByteBufPayload;
import reactor.core.Exceptions;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

final class TestChannelSupport extends ChannelSupport implements Channel {

  static final String DATA_CONTENT = "testData";
  static final String METADATA_CONTENT = "testMetadata";

  final Throwable error;

  TestChannelSupport(@Nullable Throwable error, StreamIdProvider streamIdProvider, Connection connection,
          int mtu, int maxFrameLength, int maxInboundPayloadSize, @Nullable RequestInterceptor requestInterceptor) {
    super(mtu, maxFrameLength, maxInboundPayloadSize, PayloadDecoder.ZERO_COPY, connection, streamIdProvider, (__) -> requestInterceptor);
    this.error = error;
  }

  @Override
  public TestConnection getConnection() {
    return (TestConnection) super.getConnection();
  }

  static Payload genericPayload(LeaksTrackingByteBufAllocator allocator) {
    ByteBuf data = allocator.buffer();
    data.writeCharSequence(DATA_CONTENT, CharsetUtil.UTF_8);

    ByteBuf metadata = allocator.buffer();
    metadata.writeCharSequence(METADATA_CONTENT, CharsetUtil.UTF_8);

    return ByteBufPayload.create(data, metadata);
  }

  static Payload fixedSizePayload(LeaksTrackingByteBufAllocator allocator, int contentSize) {
    final int dataSize = ThreadLocalRandom.current().nextInt(0, contentSize);
    final byte[] dataBytes = new byte[dataSize];
    ThreadLocalRandom.current().nextBytes(dataBytes);
    ByteBuf data = allocator.buffer(dataSize);
    data.writeBytes(dataBytes);

    ByteBuf metadata;
    int metadataSize = contentSize - dataSize;
    if (metadataSize > 0) {
      final byte[] metadataBytes = new byte[metadataSize];
      metadata = allocator.buffer(metadataSize);
      metadata.writeBytes(metadataBytes);
    }
    else {
      metadata = ThreadLocalRandom.current().nextBoolean() ? Unpooled.EMPTY_BUFFER : null;
    }

    return ByteBufPayload.create(data, metadata);
  }

  static Payload randomPayload(LeaksTrackingByteBufAllocator allocator) {
    boolean hasMetadata = ThreadLocalRandom.current().nextBoolean();
    ByteBuf metadataByteBuf;
    if (hasMetadata) {
      byte[] randomMetadata = new byte[ThreadLocalRandom.current().nextInt(0, 512)];
      ThreadLocalRandom.current().nextBytes(randomMetadata);
      metadataByteBuf = allocator.buffer().writeBytes(randomMetadata);
    }
    else {
      metadataByteBuf = null;
    }
    byte[] randomData = new byte[ThreadLocalRandom.current().nextInt(512, 1024)];
    ThreadLocalRandom.current().nextBytes(randomData);

    ByteBuf dataByteBuf = allocator.buffer().writeBytes(randomData);
    return ByteBufPayload.create(dataByteBuf, metadataByteBuf);
  }

  static Payload randomMetadataOnlyPayload(LeaksTrackingByteBufAllocator allocator) {
    byte[] randomMetadata = new byte[ThreadLocalRandom.current().nextInt(512, 1024)];
    ThreadLocalRandom.current().nextBytes(randomMetadata);
    ByteBuf metadataByteBuf = allocator.buffer().writeBytes(randomMetadata);

    return ByteBufPayload.create(Unpooled.EMPTY_BUFFER, metadataByteBuf);
  }

  static ArrayList<ByteBuf> prepareFragments(
          LeaksTrackingByteBufAllocator allocator, int mtu, Payload payload) {

    return prepareFragments(allocator, mtu, payload, FrameType.NEXT_COMPLETE);
  }

  static ArrayList<ByteBuf> prepareFragments(
          LeaksTrackingByteBufAllocator allocator, int mtu, Payload payload, FrameType frameType) {

    boolean hasMetadata = payload.hasMetadata();
    ByteBuf data = payload.sliceData();
    ByteBuf metadata = payload.sliceMetadata();
    ArrayList<ByteBuf> fragments = new ArrayList<>();

    fragments.add(
            frameType.hasInitialRequestN()
                    ? FragmentationUtils.encodeFirstFragment(
                    allocator, mtu, 1L, frameType, 1, hasMetadata, metadata, data)
                    : FragmentationUtils.encodeFirstFragment(
                            allocator, mtu, frameType, 1, hasMetadata, metadata, data));

    while (metadata.isReadable() || data.isReadable()) {
      fragments.add(
              FragmentationUtils.encodeFollowsFragment(allocator, mtu, 1, true, metadata, data));
    }

    return fragments;
  }

  @Override
  public synchronized int getNextStreamId() {
    int nextStreamId = super.getNextStreamId();

    if (error != null) {
      throw Exceptions.propagate(error);
    }

    return nextStreamId;
  }

  @Override
  public synchronized int addAndGetNextStreamId(FrameHandler frameHandler) {
    int nextStreamId = super.addAndGetNextStreamId(frameHandler);

    if (error != null) {
      super.remove(nextStreamId, frameHandler);
      throw Exceptions.propagate(error);
    }

    return nextStreamId;
  }

  public static TestChannelSupport client(
          @Nullable Throwable e, @Nullable RequestInterceptor requestInterceptor) {
    return client(
            new TestConnection(
                    LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT)),
            0,
            FRAME_LENGTH_MASK,
            Integer.MAX_VALUE,
            requestInterceptor,
            e);
  }

  public static TestChannelSupport client(@Nullable Throwable e) {
    return client(0, FRAME_LENGTH_MASK, Integer.MAX_VALUE, e);
  }

  public static TestChannelSupport client(
          int mtu, int maxFrameLength, int maxInboundPayloadSize, @Nullable Throwable e) {
    return client(
            new TestConnection(
                    LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT)),
            mtu,
            maxFrameLength,
            maxInboundPayloadSize,
            null,
            e);
  }

  public static TestChannelSupport client(
          TestConnection duplexConnection,
          int mtu,
          int maxFrameLength,
          int maxInboundPayloadSize) {
    return client(duplexConnection, mtu, maxFrameLength, maxInboundPayloadSize, null);
  }

  public static TestChannelSupport client(
          TestConnection duplexConnection,
          int mtu,
          int maxFrameLength,
          int maxInboundPayloadSize,
          @Nullable RequestInterceptor requestInterceptor) {
    return client(
            duplexConnection, mtu, maxFrameLength, maxInboundPayloadSize, requestInterceptor, null);
  }

  public static TestChannelSupport client(
          TestConnection duplexConnection,
          int mtu,
          int maxFrameLength,
          int maxInboundPayloadSize,
          @Nullable RequestInterceptor requestInterceptor,
          @Nullable Throwable e) {
    return new TestChannelSupport(
            e,
            StreamIdProvider.forClient(),
            duplexConnection,
            mtu,
            maxFrameLength,
            maxInboundPayloadSize,
            requestInterceptor);
  }

  public static TestChannelSupport client(
          int mtu, int maxFrameLength, int maxInboundPayloadSize) {
    return client(mtu, maxFrameLength, maxInboundPayloadSize, null);
  }

  public static TestChannelSupport client(int mtu, int maxFrameLength) {
    return client(mtu, maxFrameLength, Integer.MAX_VALUE);
  }

  public static TestChannelSupport client(int mtu) {
    return client(mtu, FRAME_LENGTH_MASK);
  }

  public static TestChannelSupport client() {
    return client(0);
  }

  public static TestChannelSupport client(RequestInterceptor requestInterceptor) {
    return client(
            new TestConnection(
                    LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT)),
            0,
            FRAME_LENGTH_MASK,
            Integer.MAX_VALUE,
            requestInterceptor);
  }

  public TestChannelSupport assertNoActiveStreams() {
    Assertions.assertThat(activeStreams).isEmpty();
    return this;
  }

  public TestChannelSupport assertHasStream(int i, FrameHandler stream) {
    Assertions.assertThat(activeStreams).containsEntry(i, stream);
    return this;
  }

  @Override
  public LeaksTrackingByteBufAllocator getAllocator() {
    return (LeaksTrackingByteBufAllocator) super.getAllocator();
  }
}
