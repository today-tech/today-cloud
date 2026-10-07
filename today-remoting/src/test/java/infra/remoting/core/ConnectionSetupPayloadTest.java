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

import org.junit.jupiter.api.Test;

import infra.remoting.ConnectionSetupPayload;
import infra.remoting.Payload;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.util.DefaultPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

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
