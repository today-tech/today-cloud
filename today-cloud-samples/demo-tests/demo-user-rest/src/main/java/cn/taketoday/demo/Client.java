/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.demo;

import cn.taketoday.cloud.protocol.http.HttpServiceRegistry;
import cn.taketoday.demo.model.User;
import cn.taketoday.demo.service.UserService;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/10/18 00:06
 */
public class Client {

  public static void main(String[] args) {
    HttpServiceRegistry serviceRegistry = HttpServiceRegistry.ofURL("http://localhost:8080/services");
    UserService userService = serviceRegistry.lookupService(UserService.class);

    String ret = userService.hello("today rpc");
    System.out.println(ret);

    User byId = userService.getById(1);
    System.out.println(byId);

    try {
      userService.throwEx();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }

    userService.notFound();

//    DefaultUserService service = new DefaultUserService();
//    long start = System.currentTimeMillis();
//    for (int i = 0; i < 100_000_000; i++) {
//      service.hello("121121");
//    }
//    System.out.println(System.currentTimeMillis() - start + "ms");
//    start = System.currentTimeMillis();
//
////    for (int i = 0; i < 100/*_000_000*/; i++) {
//    for (int i = 0; i < 100_000/*_000*/; i++) {
//      userService.hello("121121");
//    }
//
//    System.out.println(System.currentTimeMillis() - start + "ms");

  }

}
