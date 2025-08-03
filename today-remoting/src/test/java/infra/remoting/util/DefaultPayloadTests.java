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

package infra.remoting.util;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import infra.remoting.Payload;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultPayloadTests {
  public static final String DATA_VAL = "data";
  public static final String METADATA_VAL = "metadata";

  @Test
  public void testReuse() {
    Payload p = DefaultPayload.create(DATA_VAL, METADATA_VAL);
    assertDataAndMetadata(p, DATA_VAL, METADATA_VAL);
    assertDataAndMetadata(p, DATA_VAL, METADATA_VAL);
  }

  public void assertDataAndMetadata(Payload p, String dataVal, String metadataVal) {
    assertThat(p.getDataUtf8()).describedAs("Unexpected data.").isEqualTo(dataVal);
    if (metadataVal == null) {
      assertThat(p.hasMetadata()).describedAs("Non-null metadata").isEqualTo(false);
    }
    else {
      assertThat(p.hasMetadata()).describedAs("Null metadata").isEqualTo(true);
      assertThat(p.getMetadataUtf8()).describedAs("Unexpected metadata.").isEqualTo(metadataVal);
    }
  }

  @Test
  public void staticMethods() {
    assertDataAndMetadata(DefaultPayload.create(DATA_VAL, METADATA_VAL), DATA_VAL, METADATA_VAL);
    assertDataAndMetadata(DefaultPayload.create(DATA_VAL), DATA_VAL, null);
  }

  @Test
  public void shouldIndicateThatItHasNotMetadata() {
    Payload payload = DefaultPayload.create("data");

    assertThat(payload.hasMetadata()).isFalse();
  }

  @Test
  public void shouldIndicateThatItHasMetadata1() {
    Payload payload =
            DefaultPayload.create(Unpooled.wrappedBuffer("data".getBytes()), Unpooled.EMPTY_BUFFER);

    assertThat(payload.hasMetadata()).isTrue();
  }

  @Test
  public void shouldIndicateThatItHasMetadata2() {
    Payload payload =
            DefaultPayload.create(ByteBuffer.wrap("data".getBytes()), ByteBuffer.allocate(0));

    assertThat(payload.hasMetadata()).isTrue();
  }

  @Test
  public void shouldReleaseGivenByteBufDataAndMetadataUpOnPayloadCreation() {
    LeaksTrackingByteBufAllocator allocator =
            LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
    for (byte i = 0; i < 126; i++) {
      ByteBuf data = allocator.buffer();
      data.writeByte(i);

      boolean metadataPresent = ThreadLocalRandom.current().nextBoolean();
      ByteBuf metadata = null;
      if (metadataPresent) {
        metadata = allocator.buffer();
        metadata.writeByte(i + 1);
      }

      Payload payload = DefaultPayload.create(data, metadata);

      assertThat(payload.getData()).isEqualTo(ByteBuffer.wrap(new byte[] { i }));

      assertThat(payload.getMetadata())
              .isEqualTo(
                      metadataPresent
                              ? ByteBuffer.wrap(new byte[] { (byte) (i + 1) })
                              : DefaultPayload.EMPTY_BUFFER);
      allocator.assertHasNoLeaks();
    }
  }
}
