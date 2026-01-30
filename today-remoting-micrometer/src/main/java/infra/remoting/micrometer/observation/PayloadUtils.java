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

package infra.remoting.micrometer.observation;

import java.util.Set;

import infra.remoting.Payload;
import infra.remoting.util.ByteBufPayload;
import infra.remoting.util.DefaultPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

final class PayloadUtils {

  private PayloadUtils() {
    throw new IllegalStateException("Can't instantiate a utility class");
  }

  static ByteBuf cleanTracingMetadata(Payload payload, Set<String> fields) {
    return Unpooled.EMPTY_BUFFER;
  }

  static Payload payload(Payload payload, ByteBuf metadata) {
    final Payload newPayload;
    try {
      if (payload instanceof ByteBufPayload) {
        newPayload = ByteBufPayload.create(payload.data().retain(), metadata);
      }
      else {
        newPayload = DefaultPayload.create(payload.data().retain(), metadata);
      }
    }
    finally {
      payload.release();
    }
    return newPayload;
  }
}
