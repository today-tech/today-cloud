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

package cn.taketoday.cloud.demo;

import cn.taketoday.cloud.demo.model.User;
import cn.taketoday.cloud.demo.service.UserService;
import cn.taketoday.cloud.protocol.http.HttpServiceRegistry;
import cn.taketoday.cloud.provider.EnableHttpServiceProvider;
import cn.taketoday.cloud.registry.EnableHttpRegistry;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.framework.web.WebApplication;
import cn.taketoday.web.config.EnableWebMvc;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/10/17 22:50
 */
public class App {

  @EnableWebMvc
  @EnableHttpRegistry
  @EnableAutoConfiguration
  public static class Registry {

    public static void main(String[] args) {
      WebApplication.run(Registry.class, args);
    }

  }

  @EnableHttpServiceProvider
  @EnableWebMvc
  @Configuration
  @EnableAutoConfiguration
  public static class RpcServer {

    public static void main(String[] args) {
      WebApplication.run(RpcServer.class, args);
    }

  }

  public static class RpcClient {

    public static void main(String[] args) {
      HttpServiceRegistry serviceRegistry = HttpServiceRegistry.ofURL("http://localhost:8080/services");
      UserService userService = serviceRegistry.lookup(UserService.class);

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

}
