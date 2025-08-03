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
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import infra.remoting.Payload;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.FrameLengthCodec;
import infra.remoting.util.DefaultPayload;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_SIZE;

class PayloadValidationUtilsTest {

  @Test
  void shouldBeValidFrameWithNoFragmentation() {
    int maxFrameLength =
            ThreadLocalRandom.current().nextInt(64, FrameLengthCodec.FRAME_LENGTH_MASK);
    byte[] data = new byte[maxFrameLength - FRAME_LENGTH_SIZE - FrameHeaderCodec.size()];
    ThreadLocalRandom.current().nextBytes(data);
    final Payload payload = DefaultPayload.create(data);

    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, false))
            .isTrue();
    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, true))
            .isFalse();
  }

  @Test
  void shouldBeValidFrameWithNoFragmentation1() {
    int maxFrameLength =
            ThreadLocalRandom.current().nextInt(64, FrameLengthCodec.FRAME_LENGTH_MASK);
    byte[] data =
            new byte[maxFrameLength - FRAME_LENGTH_SIZE - Integer.BYTES - FrameHeaderCodec.size()];
    ThreadLocalRandom.current().nextBytes(data);
    final Payload payload = DefaultPayload.create(data);

    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, false))
            .isTrue();
    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, true))
            .isTrue();
  }

  @Test
  void shouldBeInValidFrameWithNoFragmentation() {
    int maxFrameLength =
            ThreadLocalRandom.current().nextInt(64, FrameLengthCodec.FRAME_LENGTH_MASK);
    byte[] data = new byte[maxFrameLength - FRAME_LENGTH_SIZE - FrameHeaderCodec.size() + 1];
    ThreadLocalRandom.current().nextBytes(data);
    final Payload payload = DefaultPayload.create(data);

    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, true))
            .isFalse();
    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, false))
            .isFalse();
  }

  @Test
  void shouldBeValidFrameWithNoFragmentation0() {
    int maxFrameLength =
            ThreadLocalRandom.current().nextInt(64, FrameLengthCodec.FRAME_LENGTH_MASK);
    byte[] metadata = new byte[maxFrameLength / 2];
    byte[] data =
            new byte
                    [(maxFrameLength / 2 + 1)
                    - FRAME_LENGTH_SIZE
                    - FrameHeaderCodec.size()
                    - FrameHeaderCodec.size()];
    ThreadLocalRandom.current().nextBytes(data);
    ThreadLocalRandom.current().nextBytes(metadata);
    final Payload payload = DefaultPayload.create(data, metadata);

    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, false))
            .isTrue();
    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, true))
            .isFalse();
  }

  @Test
  void shouldBeInValidFrameWithNoFragmentation1() {
    int maxFrameLength =
            ThreadLocalRandom.current().nextInt(64, FrameLengthCodec.FRAME_LENGTH_MASK);
    byte[] metadata = new byte[maxFrameLength];
    byte[] data = new byte[maxFrameLength];
    ThreadLocalRandom.current().nextBytes(metadata);
    ThreadLocalRandom.current().nextBytes(data);
    final Payload payload = DefaultPayload.create(data, metadata);

    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, true))
            .isFalse();
    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, false))
            .isFalse();
  }

  @Test
  void shouldBeValidFrameWithNoFragmentation2() {
    int maxFrameLength =
            ThreadLocalRandom.current().nextInt(64, FrameLengthCodec.FRAME_LENGTH_MASK);
    byte[] metadata = new byte[1];
    byte[] data = new byte[1];
    ThreadLocalRandom.current().nextBytes(metadata);
    ThreadLocalRandom.current().nextBytes(data);
    final Payload payload = DefaultPayload.create(data, metadata);

    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, true))
            .isTrue();
    Assertions.assertThat(PayloadValidationUtils.isValid(0, maxFrameLength, payload, false))
            .isTrue();
  }

  @Test
  void shouldBeValidFrameWithNoFragmentation3() {
    int maxFrameLength =
            ThreadLocalRandom.current().nextInt(64, FrameLengthCodec.FRAME_LENGTH_MASK);
    byte[] metadata = new byte[maxFrameLength];
    byte[] data = new byte[maxFrameLength];
    ThreadLocalRandom.current().nextBytes(metadata);
    ThreadLocalRandom.current().nextBytes(data);
    final Payload payload = DefaultPayload.create(data, metadata);

    Assertions.assertThat(PayloadValidationUtils.isValid(64, maxFrameLength, payload, true))
            .isTrue();
    Assertions.assertThat(PayloadValidationUtils.isValid(64, maxFrameLength, payload, false))
            .isTrue();
  }

  @Test
  void shouldBeValidFrameWithNoFragmentation4() {
    int maxFrameLength =
            ThreadLocalRandom.current().nextInt(64, FrameLengthCodec.FRAME_LENGTH_MASK);
    byte[] metadata = new byte[1];
    byte[] data = new byte[1];
    ThreadLocalRandom.current().nextBytes(metadata);
    ThreadLocalRandom.current().nextBytes(data);
    final Payload payload = DefaultPayload.create(data, metadata);

    Assertions.assertThat(PayloadValidationUtils.isValid(64, maxFrameLength, payload, true))
            .isTrue();
    Assertions.assertThat(PayloadValidationUtils.isValid(64, maxFrameLength, payload, false))
            .isTrue();
  }
}
