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

package cn.taketoday.demo.service;

import cn.taketoday.demo.model.User;
import infra.stereotype.Service;

/**
 * @author TODAY 2021/7/3 22:46
 */
@Service
public class DefaultUserService implements UserService {

  @Override
  public String hello(String text) {
    return "Hello " + text;
  }

  @Override
  public User getById(Integer id) {
    final User user = new User();
    user.setAge(23);
    user.setId(id);
    user.setName("TODAY");
    return user;
  }

  @Override
  public void throwEx() {
    throw new RuntimeException("throwEx");
  }

  @Override
  public void notFound() {

  }
}
