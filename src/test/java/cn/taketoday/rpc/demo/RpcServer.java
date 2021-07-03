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

package cn.taketoday.rpc.demo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.framework.ServletWebServerApplicationContext;
import cn.taketoday.framework.StandardWebServerApplicationContext;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.framework.server.TomcatServer;
import cn.taketoday.rpc.ServiceRegistry;
import cn.taketoday.rpc.config.ServiceConfig;
import cn.taketoday.rpc.demo.service.UserService;
import cn.taketoday.rpc.demo.service.impl.DefaultUserService;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.rpc.server.ServiceEndpoint;
import cn.taketoday.web.annotation.RestController;

/**
 * @author TODAY 2021/7/3 22:48
 */
public class RpcServer {

  public static void main(String[] args) throws IOException {
    final ServletWebServerApplicationContext context = new ServletWebServerApplicationContext(RpcServer.class, args);
    context.importBeans(RpcServerConfig.class);

    new WebApplication(context).run(args);
    System.in.read();
  }

  static class RpcServerConfig {

    @Singleton
    TomcatServer tomcatServer() {
      final TomcatServer tomcatServer = new TomcatServer();
      tomcatServer.setPort(9000);
      return tomcatServer;
    }

    @RestController
    ServiceEndpoint endpoint() throws IOException {
      final ServiceDefinition definition = new ServiceDefinition();

      definition.setHost("localhost");
      definition.setPort(9000);
      definition.setName(UserService.class.getName());
      ServiceRegistry.register(definition);
      Map<String, Object> local = new HashMap<>();

      local.put(UserService.class.getName(), new DefaultUserService());
      return new ServiceEndpoint(local);
    }

  }

}
