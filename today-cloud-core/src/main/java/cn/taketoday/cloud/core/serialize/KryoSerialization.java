/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.cloud.core.serialize;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.taketoday.lang.Assert;

/**
 * Kryo Serialization
 *
 * @author TODAY 2021/7/21 22:12
 */
public class KryoSerialization<T> extends Serialization<T> {
  private final Kryo kryo;

  public KryoSerialization() {
    this(new Kryo());
  }

  public KryoSerialization(Kryo kryo) {
    Assert.notNull(kryo, "Kryo must not be null");
    this.kryo = kryo;
  }

  @Override
  public void serialize(Object object, OutputStream output) throws IOException {
    Output kryoOutput = new Output(output);
    kryo.writeClassAndObject(kryoOutput, object);
  }

  @Override
  public Object deserializeInternal(InputStream inputStream) {
    Input input = new Input(inputStream);
    return kryo.readClassAndObject(input);
  }

  public Kryo getKryo() {
    return kryo;
  }

}
