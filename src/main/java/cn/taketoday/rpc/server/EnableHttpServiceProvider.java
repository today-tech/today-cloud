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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.framework.server.AbstractWebServer;
import cn.taketoday.rpc.RpcRequest;
import cn.taketoday.rpc.RpcResponse;
import cn.taketoday.rpc.ServiceRegistry;
import cn.taketoday.rpc.protocol.http.HttpServiceRegistry;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.rpc.serialize.JdkSerialization;
import cn.taketoday.rpc.serialize.Serialization;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.view.ResultHandlers;
import cn.taketoday.web.view.RuntimeResultHandler;

/**
 * @author TODAY 2021/7/11 15:31
 */
@Import(HttpServiceProviderConfig.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableHttpServiceProvider {

}

final class HttpServiceProviderConfig {
  private static final Logger log = LoggerFactory.getLogger(HttpServiceProviderConfig.class);

  @MissingBean(type = ServiceRegistry.class)
  static HttpServiceRegistry serviceRegistry(@Env("registry.url") String registryURL) {
    return HttpServiceRegistry.ofURL(registryURL);
  }

  @MissingBean(type = Serialization.class)
  static JdkSerialization<RpcRequest> requestSerialization() {
    return new JdkSerialization<>();
  }

  @Singleton
  @Order(Ordered.HIGHEST_PRECEDENCE << 1)
  static HandlerRegistry handlerRegistry(
          AbstractWebServer server, ApplicationContext context,
          ServiceRegistry serviceRegistry, Serialization<RpcRequest> requestSerialization,
          @Env(value = "service.provider.uri", defaultValue = "/provider") String serviceProviderPath) {

    List<Object> services = context.getAnnotatedBeans(Service.class);
    HashMap<String, Object> local = new HashMap<>();
    ArrayList<ServiceDefinition> definitions = new ArrayList<>();
    for (Object service : services) {
      Class<Object> serviceImpl = ClassUtils.getUserClass(service);
      Class<?>[] interfaces = serviceImpl.getInterfaces();
      if (ObjectUtils.isEmpty(interfaces)) {
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

      log.info("add service: [{}] to interface: [{}]", service, definition.getName());
      definitions.add(definition);
      local.put(interfaceToUse.getName(), service); // register object
    }

    log.info("Registering services to registry: [{}]", serviceRegistry);
    serviceRegistry.register(definitions); // register to registry

    return new ServiceProviderRegistry(serviceProviderPath, new HttpServiceProviderEndpoint(local, requestSerialization));
  }

  @Singleton
  static ResultHandlers resultHandlers(Serialization<RpcRequest> requestSerialization) {
    ResultHandlers resultHandlers = new ResultHandlers();
    resultHandlers.addHandlers(new SerializationResultHandler(requestSerialization));
    return resultHandlers;
  }

  @Singleton
  static RpcHandlerExceptionHandler rpcHttpExceptionHandler() {
    return new RpcHandlerExceptionHandler();
  }

  /** HandlerRegistry for rpc */
  static final class ServiceProviderRegistry implements HandlerRegistry {
    final String serviceProviderPath;
    final HttpServiceProviderEndpoint providerEndpoint;

    ServiceProviderRegistry(String serviceProviderPath, HttpServiceProviderEndpoint providerEndpoint) {
      this.serviceProviderPath = serviceProviderPath;
      this.providerEndpoint = providerEndpoint;
    }

    @Override
    public Object lookup(RequestContext context) {
      if (context.getRequestPath().equals(serviceProviderPath)) {
        return providerEndpoint;
      }
      return null;
    }
  }

  /** RuntimeResultHandler for rpc */
  static final class SerializationResultHandler implements RuntimeResultHandler {
    private final Serialization<RpcRequest> requestSerialization;

    public SerializationResultHandler(Serialization<RpcRequest> requestSerialization) {
      this.requestSerialization = requestSerialization;
    }

    @Override
    public boolean supportsResult(Object result) {
      return result instanceof RpcResponse;
    }

    @Override
    public void handleResult(RequestContext context, Object handler, Object result) throws Throwable {
      requestSerialization.serialize(result, context.getOutputStream());
    }
  }

  /** HandlerExceptionHandler for rpc */
  static final class RpcHandlerExceptionHandler implements HandlerExceptionHandler {

    @Override
    public Object handleException(RequestContext context, Throwable exception, Object handler) throws Throwable {
      if (exception instanceof ClassNotFoundException) {
        return RpcResponse.ofThrowable(new ServiceNotFoundException());
      }
      return RpcResponse.ofThrowable(exception);
    }
  }

}
