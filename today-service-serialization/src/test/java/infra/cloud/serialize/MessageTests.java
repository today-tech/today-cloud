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

package infra.cloud.serialize;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/16 23:46
 */
class MessageTests {

  @Test
  void test() {
    User user = new User("1", 2);

//    user.write();

  }

  static class User implements Message, Serializable {
    private String name;

    private int age;

    public User() {
    }

    public User(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public int getAge() {
      return age;
    }

    public String getName() {
      return name;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public void setName(String name) {
      this.name = name;
    }

    @Override
    public void writeTo(Writable writable) {
      writable.write(name);
      writable.write(age);
    }

    @Override
    public void readFrom(Readable readable) {
      this.name = readable.readString();
      this.age = readable.readInt();
    }

  }

}