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

package infra.cloud.serialize.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import infra.cloud.serialize.Readable;
import infra.cloud.serialize.ReturnValueDeserializer;
import infra.cloud.serialize.ReturnValueSerializer;
import infra.cloud.serialize.SerializationException;
import infra.cloud.serialize.Writable;
import infra.cloud.service.ServiceMethod;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2026/3/18 11:42
 */
public class SerializableReturnValueSerialization implements ReturnValueDeserializer<Serializable>, ReturnValueSerializer<Serializable> {

  @Override
  public boolean supportsReturnValue(ServiceMethod method) {
    return method.isReturnTypeAssignableTo(Serializable.class);
  }

  @Override
  public void serialize(ServiceMethod method, Serializable value, Writable writable) throws SerializationException {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try (ObjectOutputStream stream = new ObjectOutputStream(output)) {
        stream.writeObject(value);
        stream.flush();
      }
      writable.write(output.toByteArray());
    }
    catch (IOException e) {
      throw new SerializationException(value + " serialize failed", e);
    }
  }

  @Override
  public Serializable deserialize(ServiceMethod method, Readable readable) throws SerializationException {
    try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(readable.read()))) {
      return (Serializable) stream.readObject();
    }
    catch (Exception e) {
      throw new SerializationException(method + " return value deserialize failed", e);
    }
  }

}
