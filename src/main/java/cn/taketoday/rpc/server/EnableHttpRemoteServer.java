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

package cn.taketoday.rpc.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.framework.server.AbstractWebServer;
import cn.taketoday.rpc.protocol.http.HttpServiceRegistry;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;

/**
 * @author TODAY 2021/7/11 15:31
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableHttpRemoteServer {

}

final class RpcServerConfig {
  private static final Logger log = LoggerFactory.getLogger(RpcServerConfig.class);

  @MissingBean
  HttpServiceRegistry serviceRegistry(@Env("registry.url") String registryURL) {
    return HttpServiceRegistry.ofURL(registryURL);
  }

  @RestController
  @RequestMapping("#{service.provider.uri}")
  HttpServiceEndpoint serviceProviderEndpoint(
          AbstractWebServer server, ApplicationContext context, HttpServiceRegistry serviceRegistry) {
    List<Object> services = context.getAnnotatedBeans(Service.class);
    HashMap<String, Object> local = new HashMap<>();

    for (Object service : services) {
      Class<Object> serviceImpl = ClassUtils.getUserClass(service);
      Class<?>[] interfaces = serviceImpl.getInterfaces();
      if (interfaces.length == 0) {
        continue;
      }

      Class<?> interfaceToUse = interfaces[0];
      if (interfaces.length > 1) {
        for (final Class<?> anInterface : interfaces) {
          if (anInterface.isAnnotationPresent(Service.class)) {
            interfaceToUse = anInterface;
            break;
          }
        }
      }

      ServiceDefinition definition = new ServiceDefinition();

      definition.setHost(server.getHost()); // TODO resolve target host
      definition.setPort(server.getPort());
      definition.setName(interfaceToUse.getName());
      definition.setServiceInterface(interfaceToUse);

      log.info("Registering service: [{}] to interface: [{}]", service, definition.getName());
      serviceRegistry.register(definition); // register to registry

      local.put(serviceImpl.getName(), service); // register object
    }

    return new HttpServiceEndpoint(local);
  }

}
