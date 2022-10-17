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

package cn.taketoday.cloud.provider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.JdkSerialization;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.registry.ServiceDefinition;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.cloud.registry.ServiceRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.stereotype.Service;
import cn.taketoday.stereotype.Singleton;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Enable service provider based on HTTP
 *
 * @author TODAY 2021/7/11 15:31
 */
@Import(HttpServiceProviderConfig.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableHttpServiceProvider {

}

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServerProperties.class)
final class HttpServiceProviderConfig {
  private static final Logger log = LoggerFactory.getLogger(HttpServiceProviderConfig.class);

//  @MissingBean
//  static ServiceRegistry serviceRegistry(@Value("${registry.url}") String registryURL) {
//    return HttpServiceRegistry.ofURL(registryURL);
//  }

  @MissingBean
  static Serialization<RpcRequest> requestSerialization() {
    return new JdkSerialization<>();
  }

  private final PathPatternParser pathPatternParser = PathPatternParser.defaultInstance;

  @Singleton
  HandlerMapping httpServiceHandlerMapping(
          ServerProperties serverProperties, ApplicationContext context,
          ServiceRegistry serviceRegistry, Serialization<RpcRequest> requestSerialization,
          @Value("${service.provider.uri:/provider}") String serviceProviderPath) throws UnknownHostException {

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

      if (serverProperties.getAddress() != null) {
        definition.setHost(serverProperties.getAddress().getHostName());
      }
      else {
        InetAddress localHost = InetAddress.getLocalHost();
        definition.setHost(localHost.getHostName()); // TODO resolve target host
      }
      definition.setPort(serverProperties.getPort());
      definition.setName(interfaceToUse.getName());
      definition.setServiceInterface(interfaceToUse);

      log.info("add service: [{}] to interface: [{}]", service, definition.getName());
      definitions.add(definition);
      local.put(interfaceToUse.getName(), service); // register object
    }

    log.info("Registering services to registry: [{}]", serviceRegistry);
    serviceRegistry.register(definitions); // register to registry

    PathPattern pathPattern = pathPatternParser.parse(serviceProviderPath);
    return new ServiceProviderRegistry(pathPattern, serviceRegistry, definitions,
            new HttpServiceProviderEndpoint(local, requestSerialization));
  }

  @Singleton
  static ReturnValueHandlerManager returnValueHandlerManager(Serialization<RpcRequest> requestSerialization) {
    ReturnValueHandlerManager resultHandlers = new ReturnValueHandlerManager();
    resultHandlers.addHandlers(new SerializationResultHandler(requestSerialization));
    return resultHandlers;
  }

  @Singleton
  static RpcHandlerExceptionHandler rpcHttpExceptionHandler() {
    return new RpcHandlerExceptionHandler();
  }

  /** HandlerMapping for rpc */
  static final class ServiceProviderRegistry implements HandlerMapping, DisposableBean, Ordered {

    final PathPattern serviceProviderPath;
    final ServiceRegistry serviceRegistry;
    final List<ServiceDefinition> definitions;
    final HttpServiceProviderEndpoint providerEndpoint;

    ServiceProviderRegistry(PathPattern serviceProviderPath, ServiceRegistry serviceRegistry,
            List<ServiceDefinition> definitions, HttpServiceProviderEndpoint providerEndpoint) {
      this.definitions = definitions;
      this.serviceRegistry = serviceRegistry;
      this.providerEndpoint = providerEndpoint;
      this.serviceProviderPath = serviceProviderPath;
    }

    @Override
    public Object getHandler(RequestContext request) throws Exception {
      if (serviceProviderPath.matches(request.getLookupPath())) {
        return providerEndpoint;
      }
      return null;
    }

    /**
     * Go offline to delete the service registered on the machine
     */
    @Override
    public void destroy() throws Exception {
      serviceRegistry.unregister(definitions);
    }

    @Override
    public int getOrder() {
      return HIGHEST_PRECEDENCE;
    }

  }

  /** RuntimeResultHandler for rpc */
  static final class SerializationResultHandler implements ReturnValueHandler {
    private final Serialization<RpcRequest> requestSerialization;

    public SerializationResultHandler(Serialization<RpcRequest> requestSerialization) {
      this.requestSerialization = requestSerialization;
    }

    @Override
    public boolean supportsHandler(Object handler) {
      return handler instanceof HttpServiceProviderEndpoint;
    }

    @Override
    public boolean supportsReturnValue(Object returnValue) {
      return returnValue instanceof RpcResponse;
    }

    @Override
    public void handleReturnValue(RequestContext context, Object handler, Object returnValue) throws Exception {
      requestSerialization.serialize(returnValue, context.getOutputStream());
    }
  }

  /** HandlerExceptionHandler for rpc */
  static final class RpcHandlerExceptionHandler implements HandlerExceptionHandler {

    @Override
    public Object handleException(RequestContext context, Throwable exception, Object handler) {
      if (exception instanceof ClassNotFoundException) {
        return RpcResponse.ofThrowable(new ServiceNotFoundException());
      }
      return RpcResponse.ofThrowable(exception);
    }
  }

}
