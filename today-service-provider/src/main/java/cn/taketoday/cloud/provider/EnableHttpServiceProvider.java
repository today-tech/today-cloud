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
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.JdkSerialization;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.protocol.http.HttpServiceRegistry;
import cn.taketoday.cloud.registry.RegistryProperties;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.cloud.registry.ServiceRegistry;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.stereotype.Component;
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

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ ServerProperties.class, RegistryProperties.class })
class HttpServiceProviderConfig {
  private static final Logger log = LoggerFactory.getLogger(HttpServiceProviderConfig.class);

  @MissingBean
  static ServiceRegistry serviceRegistry(RegistryProperties properties, Serialization<RpcResponse> serialization) {
    return HttpServiceRegistry.ofURL(properties.getUrl(), serialization);
  }

  @MissingBean
  static Serialization requestSerialization() {
    return new JdkSerialization<>();
  }

  @Singleton
  HandlerMapping httpServiceHandlerMapping(Serialization<RpcRequest> requestSerialization, LocalServiceHolder serviceHolder) {
    return new ServiceProviderRegistry(
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

  @Singleton
  static LocalServiceHolder localServiceHolder(@Value("${server.port}") int port) {
    LocalServiceHolder holder = new LocalServiceHolder();
    holder.setPort(port);
    return holder;
  }

  @Component
  static ServiceProviderLifecycle serviceProviderLifecycle(ServiceRegistry serviceRegistry, LocalServiceHolder serviceHolder) {
    return new ServiceProviderLifecycle(serviceRegistry, serviceHolder);
  }

  static class ServiceProviderLifecycle implements SmartLifecycle {
    final ServiceRegistry serviceRegistry;
    final LocalServiceHolder serviceHolder;

    private final AtomicBoolean started = new AtomicBoolean();

    ServiceProviderLifecycle(ServiceRegistry serviceRegistry, LocalServiceHolder serviceHolder) {
      this.serviceRegistry = serviceRegistry;
      this.serviceHolder = serviceHolder;
    }

    @Override
    public void start() {
      if (started.compareAndSet(false, true)) {
        log.info("Registering services to registry: [{}]", serviceRegistry);
        serviceRegistry.register(serviceHolder.getServices()); // register to registry
      }
    }

    @Override
    public void stop() {
      throw new UnsupportedOperationException("Stop must not be invoked directly");
    }

    /**
     * Go offline to delete the service registered on the machine
     */
    @Override
    public void stop(Runnable callback) {
      if (started.compareAndSet(true, false)) {
        log.info("Un-Registering services: [{}]", serviceRegistry);
        try {
          serviceRegistry.unregister(serviceHolder.getServices());
        }
        finally {
          callback.run();
        }
      }
    }

    @Override
    public boolean isRunning() {
      return started.get();
    }

  }

  /** HandlerMapping for rpc */
  static final class ServiceProviderRegistry implements HandlerMapping, Ordered {

    final HttpServiceProviderEndpoint providerEndpoint;

    ServiceProviderRegistry(HttpServiceProviderEndpoint providerEndpoint) {
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
