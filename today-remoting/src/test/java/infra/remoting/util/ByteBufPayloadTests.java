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

package infra.remoting.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import infra.remoting.Payload;
import io.netty.buffer.Unpooled;
import io.netty.util.IllegalReferenceCountException;

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
