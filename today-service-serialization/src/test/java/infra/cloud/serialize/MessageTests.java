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
    public void writeTo(Output output) {
      output.write(name);
      output.write(age);
    }

    @Override
    public void readFrom(Input input) {
      this.name = input.readString();
      this.age = input.readInt();
    }

  }

}