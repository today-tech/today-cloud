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

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.Payload;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.util.DefaultPayload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionSetupPayloadTest {
  private static final int KEEP_ALIVE_INTERVAL = 5;
  private static final int KEEP_ALIVE_MAX_LIFETIME = 500;
  private static final String METADATA_TYPE = "metadata_type";
  private static final String DATA_TYPE = "data_type";

  @Test
  void testSetupPayloadWithDataMetadata() {
    ByteBuf data = Unpooled.wrappedBuffer(new byte[] { 5, 4, 3 });
    ByteBuf metadata = Unpooled.wrappedBuffer(new byte[] { 2, 1, 0 });
    Payload payload = DefaultPayload.create(data, metadata);
    boolean leaseEnabled = true;

    ByteBuf frame = encodeSetupFrame(leaseEnabled, payload);
    ConnectionSetupPayload setupPayload = new DefaultConnectionSetupPayload(frame);

    assertTrue(setupPayload.willClientHonorLease());
    assertEquals(KEEP_ALIVE_INTERVAL, setupPayload.keepAliveInterval());
    assertEquals(KEEP_ALIVE_MAX_LIFETIME, setupPayload.keepAliveMaxLifetime());
    assertEquals(METADATA_TYPE, SetupFrameCodec.metadataMimeType(frame));
    assertEquals(DATA_TYPE, SetupFrameCodec.dataMimeType(frame));
    assertTrue(setupPayload.hasMetadata());
    assertNotNull(setupPayload.metadata());
    assertEquals(payload.metadata(), setupPayload.metadata());
    assertEquals(payload.data(), setupPayload.data());
    frame.release();
  }

  @Test
  void testSetupPayloadWithNoMetadata() {
    ByteBuf data = Unpooled.wrappedBuffer(new byte[] { 5, 4, 3 });
    ByteBuf metadata = null;
    Payload payload = DefaultPayload.create(data, metadata);
    boolean leaseEnabled = false;

    ByteBuf frame = encodeSetupFrame(leaseEnabled, payload);
    ConnectionSetupPayload setupPayload = new DefaultConnectionSetupPayload(frame);

    assertFalse(setupPayload.willClientHonorLease());
    assertFalse(setupPayload.hasMetadata());
    assertNotNull(setupPayload.metadata());
    assertEquals(0, setupPayload.metadata().readableBytes());
    assertEquals(payload.data(), setupPayload.data());
    frame.release();
  }

  @Test
  void testSetupPayloadWithEmptyMetadata() {
    ByteBuf data = Unpooled.wrappedBuffer(new byte[] { 5, 4, 3 });
    ByteBuf metadata = Unpooled.EMPTY_BUFFER;
    Payload payload = DefaultPayload.create(data, metadata);
    boolean leaseEnabled = false;

    ByteBuf frame = encodeSetupFrame(leaseEnabled, payload);
    ConnectionSetupPayload setupPayload = new DefaultConnectionSetupPayload(frame);

    assertFalse(setupPayload.willClientHonorLease());
    assertTrue(setupPayload.hasMetadata());
    assertNotNull(setupPayload.metadata());
    assertEquals(0, setupPayload.metadata().readableBytes());
    assertEquals(payload.data(), setupPayload.data());
    frame.release();
  }

  private static ByteBuf encodeSetupFrame(boolean leaseEnabled, Payload setupPayload) {
    return SetupFrameCodec.encode(
            ByteBufAllocator.DEFAULT,
            leaseEnabled,
            KEEP_ALIVE_INTERVAL,
            KEEP_ALIVE_MAX_LIFETIME,
            Unpooled.EMPTY_BUFFER,
            METADATA_TYPE,
            DATA_TYPE,
            setupPayload);
  }
}
