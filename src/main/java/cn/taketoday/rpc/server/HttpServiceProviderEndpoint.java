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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.context.reflect.MethodInvoker;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.Mappings;
import cn.taketoday.rpc.RpcRequest;
import cn.taketoday.rpc.RpcResponse;
import cn.taketoday.rpc.serialize.DeserializeFailedException;
import cn.taketoday.rpc.serialize.JdkSerialization;
import cn.taketoday.rpc.serialize.Serialization;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerAdapter;

/**
 * Http service-provider handler
 *
 * @author TODAY 2021/7/4 01:14
 */
public class HttpServiceProviderEndpoint implements HandlerAdapter {

  /** service mapping */
  private final Map<String, Object> serviceMapping;
  /** for serialize and deserialize */
  private Serialization<RpcRequest> serialization;
  /** fast method mapping cache */
  private final MethodMappings methodMappings = new MethodMappings();

  public HttpServiceProviderEndpoint(Map<String, Object> local) {
    this(local, new JdkSerialization<>());
  }

  public HttpServiceProviderEndpoint(Map<String, Object> serviceMapping, Serialization<RpcRequest> serialization) {
    this.serialization = serialization;
    this.serviceMapping = serviceMapping;
  }

  @Override
  public boolean supports(Object handler) {
    return handler == this;
  }

  @Override
  public Object handle(final RequestContext context, final Object handler) throws Throwable {
    final Serialization<RpcRequest> serialization = getSerialization();
    final RpcResponse response = getResponse(context, serialization);
    serialization.serialize(response, context.getOutputStream());
    return NONE_RETURN_VALUE;
  }

  /**
   * @param context
   *         http request context
   * @param serialization
   *         serialize strategy
   *
   * @return returns a {@link RpcResponse}
   */
  private RpcResponse getResponse(RequestContext context, Serialization<RpcRequest> serialization) {
    try {
      final RpcRequest request = serialization.deserialize(context.getInputStream());
      final Object service = serviceMapping.get(request.getServiceName());
      if (service == null) {
        return RpcResponse.ofThrowable(new ServiceNotFoundException());
      }
      final MethodInvoker invoker = methodMappings.get(new MethodCacheKey(request), service);
      final Object[] args = request.getArguments();
      return createResponse(service, args, invoker);
    }
    catch (IOException io) {
      return RpcResponse.ofThrowable(new DeserializeFailedException(io));
    }
    catch (ClassNotFoundException e) {
      return RpcResponse.ofThrowable(new ServiceNotFoundException(e));
    }
    catch (Throwable e) {
      return RpcResponse.ofThrowable(e);
    }
  }

  private RpcResponse createResponse(Object service, Object[] args, MethodInvoker invoker) {
    if (invoker == null) {
      return RpcResponse.ofThrowable(new ServiceNotFoundException());
    }
    try {
      return new RpcResponse(invoker.invoke(service, args));
    }
    catch (Throwable e) {
      return RpcResponse.ofThrowable(e);
    }
  }

  /**
   * set a serialization
   */
  public void setSerialization(Serialization<RpcRequest> serialization) {
    Assert.notNull(serialization, "serialization must not be null");
    this.serialization = serialization;
  }

  public Serialization<RpcRequest> getSerialization() {
    return serialization;
  }

  private static final class MethodMappings extends Mappings<MethodInvoker, Object> {

    @Override
    protected MethodInvoker createValue(final Object key, final Object service) {
      final MethodCacheKey methodCacheKey = (MethodCacheKey) key;
      final Method methodToUse = getMethod(methodCacheKey, service);
      if (methodToUse == null) {
        return null;
      }
      return MethodInvoker.create(methodToUse);
    }

    private static Method getMethod(MethodCacheKey key, final Object service) {
      final String method = key.method;
      final String[] paramTypes = key.paramTypes;
      final int parameterLength = paramTypes.length;

      final Class<Object> serviceImpl = ClassUtils.getUserClass(service);
      for (final Method serviceMethod : serviceImpl.getMethods()) {
        if (Objects.equals(serviceMethod.getName(), method)
                && parameterLength == serviceMethod.getParameterCount()) {
          int current = 0;
          boolean equals = true;
          for (final Class<?> parameterType : serviceMethod.getParameterTypes()) {
            if (!parameterType.getName().equals(paramTypes[current++])) {
              // not target method
              equals = false;
              break;
            }
          }
          if (equals) {
            return serviceMethod;
          }
        }
      }
      return null;
    }
  }

  private static class MethodCacheKey {
    public final String method;
    public final String[] paramTypes;

    MethodCacheKey(RpcRequest request) {
      this.method = request.getMethod();
      this.paramTypes = request.getParamTypes();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof MethodCacheKey))
        return false;
      final MethodCacheKey that = (MethodCacheKey) o;
      return Objects.equals(method, that.method) && Arrays.equals(paramTypes, that.paramTypes);
    }

    @Override
    public int hashCode() {
      return 31 * Objects.hash(method) + Arrays.hashCode(paramTypes);
    }
  }
}
