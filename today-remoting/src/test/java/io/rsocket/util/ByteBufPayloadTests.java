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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import io.netty.util.IllegalReferenceCountException;
import io.rsocket.Payload;

public class ByteBufPayloadTests {

  @Test
  public void shouldIndicateThatItHasMetadata() {
    Payload payload = ByteBufPayload.create("data", "metadata");

    Assertions.assertThat(payload.hasMetadata()).isTrue();
    Assertions.assertThat(payload.release()).isTrue();
  }

  @Test
  public void shouldIndicateThatItHasNotMetadata() {
    Payload payload = ByteBufPayload.create("data");

    Assertions.assertThat(payload.hasMetadata()).isFalse();
    Assertions.assertThat(payload.release()).isTrue();
  }

  @Test
  public void shouldIndicateThatItHasMetadata1() {
    Payload payload =
            ByteBufPayload.create(Unpooled.wrappedBuffer("data".getBytes()), Unpooled.EMPTY_BUFFER);

    Assertions.assertThat(payload.hasMetadata()).isTrue();
    Assertions.assertThat(payload.release()).isTrue();
  }

  @Test
  public void shouldThrowExceptionIfAccessAfterRelease() {
    Payload payload = ByteBufPayload.create("data", "metadata");

    Assertions.assertThat(payload.release()).isTrue();

    Assertions.assertThatThrownBy(payload::hasMetadata)
            .isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(payload::data).isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(payload::metadata)
            .isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(payload::sliceData)
            .isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(payload::sliceMetadata)
            .isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(payload::touch)
            .isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(() -> payload.touch("test"))
            .isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(payload::getData)
            .isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(payload::getMetadata)
            .isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(payload::getDataUtf8)
            .isInstanceOf(IllegalReferenceCountException.class);
    Assertions.assertThatThrownBy(payload::getMetadataUtf8)
            .isInstanceOf(IllegalReferenceCountException.class);
  }
}
