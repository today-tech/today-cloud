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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.framework.ServletWebServerApplicationContext;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.framework.server.AbstractWebServer;
import cn.taketoday.rpc.demo.service.UserService;
import cn.taketoday.rpc.demo.service.impl.DefaultUserService;
import cn.taketoday.rpc.protocol.http.HttpServiceRegistry;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.rpc.server.HttpServiceEndpoint;
import cn.taketoday.rpc.server.RpcServerConfig;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;

/**
 * @author TODAY 2021/7/3 22:48
 */
public class RpcServer {

  public static void main(String[] args) {
    final ServletWebServerApplicationContext context = new ServletWebServerApplicationContext(RpcServer.class, args);
    context.importBeans(RpcServerConfig.class);

    new WebApplication(context).run(args);
  }


}
