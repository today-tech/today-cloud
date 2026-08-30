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

package infra.remoting.resume;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Random UUID ResumeTokenGenerator
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/23 00:26
 */
public class RandomUUIDResumeTokenGenerator implements ResumeTokenGenerator {

  @Override
  public ByteBuf generate() {
    UUID uuid = UUID.randomUUID();
    ByteBuf bb = Unpooled.buffer(16);
    bb.writeLong(uuid.getMostSignificantBits());
    bb.writeLong(uuid.getLeastSignificantBits());
    return bb;
  }

}
