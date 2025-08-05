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
