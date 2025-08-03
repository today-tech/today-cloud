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

import infra.remoting.Payload;
import io.netty.buffer.ByteBuf;

import static infra.remoting.core.FragmentationUtils.FRAME_OFFSET;
import static infra.remoting.core.FragmentationUtils.FRAME_OFFSET_WITH_INITIAL_REQUEST_N;
import static infra.remoting.core.FragmentationUtils.FRAME_OFFSET_WITH_METADATA;
import static infra.remoting.core.FragmentationUtils.FRAME_OFFSET_WITH_METADATA_AND_INITIAL_REQUEST_N;
import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

final class PayloadValidationUtils {
  static final String INVALID_PAYLOAD_ERROR_MESSAGE =
          "The payload is too big to be send as a single frame with a max frame length %s. Consider enabling fragmentation.";

  static boolean isValid(int mtu, int maxFrameLength, Payload payload, boolean hasInitialRequestN) {

    if (mtu > 0) {
      return true;
    }

    final boolean hasMetadata = payload.hasMetadata();
    final ByteBuf data = payload.data();

    int unitSize;
    if (hasMetadata) {
      final ByteBuf metadata = payload.metadata();
      unitSize =
              (hasInitialRequestN
                      ? FRAME_OFFSET_WITH_METADATA_AND_INITIAL_REQUEST_N
                      : FRAME_OFFSET_WITH_METADATA)
                      + metadata.readableBytes()
                      + // metadata payload bytes
                      data.readableBytes(); // data payload bytes
    }
    else {
      unitSize =
              (hasInitialRequestN ? FRAME_OFFSET_WITH_INITIAL_REQUEST_N : FRAME_OFFSET)
                      + data.readableBytes(); // data payload bytes
    }

    return unitSize <= maxFrameLength;
  }

  static boolean isValidMetadata(int maxFrameLength, ByteBuf metadata) {
    return FRAME_OFFSET + metadata.readableBytes() <= maxFrameLength;
  }

  static void assertValidateSetup(int maxFrameLength, int maxInboundPayloadSize, int mtu) {
    if (maxFrameLength > FRAME_LENGTH_MASK) {
      throw new IllegalArgumentException("Configured maxFrameLength[%d] exceeds maxFrameLength limit %d".formatted(maxFrameLength, FRAME_LENGTH_MASK));
    }

    if (maxFrameLength > maxInboundPayloadSize) {
      throw new IllegalArgumentException(
              "Configured maxFrameLength[%d] exceeds maxPayloadSize[%d]".formatted(maxFrameLength, maxInboundPayloadSize));
    }

    if (mtu != 0 && mtu > maxFrameLength) {
      throw new IllegalArgumentException(
              "Configured maximumTransmissionUnit[%d] exceeds configured maxFrameLength[%d]".formatted(mtu, maxFrameLength));
    }
  }
}
