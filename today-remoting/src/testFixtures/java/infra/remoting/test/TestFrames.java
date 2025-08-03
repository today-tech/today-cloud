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

package infra.remoting.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import infra.remoting.Payload;
import infra.remoting.frame.CancelFrameCodec;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.ExtensionFrameCodec;
import infra.remoting.frame.KeepAliveFrameCodec;
import infra.remoting.frame.LeaseFrameCodec;
import infra.remoting.frame.MetadataPushFrameCodec;
import infra.remoting.frame.PayloadFrameCodec;
import infra.remoting.frame.RequestChannelFrameCodec;
import infra.remoting.frame.RequestFireAndForgetFrameCodec;
import infra.remoting.frame.RequestNFrameCodec;
import infra.remoting.frame.RequestResponseFrameCodec;
import infra.remoting.frame.RequestStreamFrameCodec;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.util.DefaultPayload;
import infra.remoting.util.EmptyPayload;

/** Test instances of all frame types. */
public final class TestFrames {
  private static final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
  private static final Payload emptyPayload = DefaultPayload.create(Unpooled.EMPTY_BUFFER);

  private TestFrames() { }

  /** @return {@link ByteBuf} representing test instance of Cancel frame */
  public static ByteBuf createTestCancelFrame() {
    return CancelFrameCodec.encode(allocator, 1);
  }

  /** @return {@link ByteBuf} representing test instance of Error frame */
  public static ByteBuf createTestErrorFrame() {
    return ErrorFrameCodec.encode(allocator, 1, new RuntimeException());
  }

  /** @return {@link ByteBuf} representing test instance of Extension frame */
  public static ByteBuf createTestExtensionFrame() {
    return ExtensionFrameCodec.encode(
            allocator, 1, 1, Unpooled.EMPTY_BUFFER, Unpooled.EMPTY_BUFFER);
  }

  /** @return {@link ByteBuf} representing test instance of Keep-Alive frame */
  public static ByteBuf createTestKeepaliveFrame() {
    return KeepAliveFrameCodec.encode(allocator, false, 1, Unpooled.EMPTY_BUFFER);
  }

  /** @return {@link ByteBuf} representing test instance of Lease frame */
  public static ByteBuf createTestLeaseFrame() {
    return LeaseFrameCodec.encode(allocator, 1, 1, null);
  }

  /** @return {@link ByteBuf} representing test instance of Metadata-Push frame */
  public static ByteBuf createTestMetadataPushFrame() {
    return MetadataPushFrameCodec.encode(allocator, Unpooled.EMPTY_BUFFER);
  }

  /** @return {@link ByteBuf} representing test instance of Payload frame */
  public static ByteBuf createTestPayloadFrame() {
    return PayloadFrameCodec.encode(allocator, 1, false, true, false, null, Unpooled.EMPTY_BUFFER);
  }

  /** @return {@link ByteBuf} representing test instance of Request-Channel frame */
  public static ByteBuf createTestRequestChannelFrame() {
    return RequestChannelFrameCodec.encode(
            allocator, 1, false, false, 1, null, Unpooled.EMPTY_BUFFER);
  }

  /** @return {@link ByteBuf} representing test instance of Fire-and-Forget frame */
  public static ByteBuf createTestRequestFireAndForgetFrame() {
    return RequestFireAndForgetFrameCodec.encode(allocator, 1, false, null, Unpooled.EMPTY_BUFFER);
  }

  /** @return {@link ByteBuf} representing test instance of Request-N frame */
  public static ByteBuf createTestRequestNFrame() {
    return RequestNFrameCodec.encode(allocator, 1, 1);
  }

  /** @return {@link ByteBuf} representing test instance of Request-Response frame */
  public static ByteBuf createTestRequestResponseFrame() {
    return RequestResponseFrameCodec.encodeReleasingPayload(allocator, 1, emptyPayload);
  }

  /** @return {@link ByteBuf} representing test instance of Request-Stream frame */
  public static ByteBuf createTestRequestStreamFrame() {
    return RequestStreamFrameCodec.encodeReleasingPayload(allocator, 1, 1L, emptyPayload);
  }

  /** @return {@link ByteBuf} representing test instance of Setup frame */
  public static ByteBuf createTestSetupFrame() {
    return SetupFrameCodec.encode(
            allocator,
            false,
            1,
            1,
            Unpooled.EMPTY_BUFFER,
            "metadataType",
            "dataType",
            EmptyPayload.INSTANCE);
  }
}
