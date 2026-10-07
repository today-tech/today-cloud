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
package infra.remoting.test;

import org.assertj.core.presentation.StandardRepresentation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.IllegalReferenceCountException;

public final class ByteBufRepresentation extends StandardRepresentation {

  @Override
  protected String fallbackToStringOf(Object object) {
    if (object instanceof ByteBuf) {
      try {
        String normalBufferString = object.toString();
        ByteBuf byteBuf = (ByteBuf) object;
        if (byteBuf.readableBytes() <= 256) {
          String prettyHexDump = ByteBufUtil.prettyHexDump(byteBuf);
          return new StringBuilder()
                  .append(normalBufferString)
                  .append("\n")
                  .append(prettyHexDump)
                  .toString();
        }
        else {
          return normalBufferString;
        }
      }
      catch (IllegalReferenceCountException e) {
        // noops
      }
    }

    return super.fallbackToStringOf(object);
  }
}
