/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.rpc.serialize;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @author TODAY 2021/7/5 22:31
 */
public class HelloKryo {
  static public void main(String[] args) throws Exception {
    Kryo kryo = new Kryo();
    kryo.register(SomeClass.class);

    SomeClass object = new SomeClass();
    object.value = "Hello Kryo!";

    Output output = new Output(new FileOutputStream("file.bin"));
    kryo.writeObject(output, object);
    output.close();

    Input input = new Input(new FileInputStream("file.bin"));
    SomeClass object2 = kryo.readObject(input, SomeClass.class);
    input.close();
  }

  static public class SomeClass {
    String value;
  }

}
