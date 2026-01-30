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

package infra.remoting.micrometer.integration.observation;

import java.util.HashMap;
import java.util.Map;

import io.micrometer.tracing.propagation.Propagator;
import io.netty.buffer.ByteBuf;

public class ByteBufSetter implements Propagator.Setter<ByteBuf> {

  final Map<String, String> map = new HashMap<>();

  @Override
  public void set(ByteBuf carrier, String key, String value) {
    map.put(key, value);
  }

}
