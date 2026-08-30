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
      unitSize = (hasInitialRequestN ? FRAME_OFFSET_WITH_METADATA_AND_INITIAL_REQUEST_N : FRAME_OFFSET_WITH_METADATA)
              + metadata.readableBytes()// metadata payload bytes
              + data.readableBytes(); // data payload bytes
    }
    else {
      unitSize = (hasInitialRequestN ? FRAME_OFFSET_WITH_INITIAL_REQUEST_N : FRAME_OFFSET) + data.readableBytes(); // data payload bytes
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
