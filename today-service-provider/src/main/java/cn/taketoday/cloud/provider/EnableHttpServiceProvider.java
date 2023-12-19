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

package cn.taketoday.cloud.provider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.JdkSerialization;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.protocol.http.HttpServiceRegistry;
import cn.taketoday.cloud.registry.HttpRegistration;
import cn.taketoday.cloud.registry.RegistryProperties;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.cloud.registry.ServiceRegistry;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.stereotype.Singleton;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.handler.ReturnValueHandlerManager;

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

@Import(ServicePublishConfig.class)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ ServerProperties.class, RegistryProperties.class })
class HttpServiceProviderConfig {

  @MissingBean
  static ServiceRegistry<HttpRegistration> serviceRegistry(RegistryProperties properties, Serialization<RpcResponse> serialization) {
    return HttpServiceRegistry.ofURL(properties.getHttpUrl(), serialization);
  }

  @MissingBean
  static Serialization requestSerialization() {
    return new JdkSerialization<>();
  }

  @Singleton
  HandlerMapping httpServiceHandlerMapping(Serialization<RpcRequest> requestSerialization, LocalServiceHolder serviceHolder) {
    return new HttpServiceProviderEndpointMapping(
            new HttpServiceProviderEndpoint(serviceHolder, requestSerialization));
  }

  @Singleton(initMethods = "registerDefaultHandlers")
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
  static final class HttpServiceProviderEndpointMapping implements HandlerMapping, Ordered {

    final HttpServiceProviderEndpoint providerEndpoint;

    HttpServiceProviderEndpointMapping(HttpServiceProviderEndpoint providerEndpoint) {
      this.providerEndpoint = providerEndpoint;
    }

    @Override
    public Object getHandler(RequestContext request) throws Exception {
      return providerEndpoint;
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
