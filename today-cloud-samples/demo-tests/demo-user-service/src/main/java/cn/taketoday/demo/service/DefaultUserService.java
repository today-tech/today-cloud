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

package cn.taketoday.demo.service;

import cn.taketoday.stereotype.Service;
import cn.taketoday.demo.model.User;

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
