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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.DeserializeFailedException;
import cn.taketoday.cloud.core.serialize.JdkSerialization;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.lang.Assert;
import cn.taketoday.reflect.MethodInvoker;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.MapCache;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;

/**
 * Http service-provider handler
 *
 * @author TODAY 2021/7/4 01:14
 */
public class HttpServiceProviderEndpoint implements HttpRequestHandler {

  /** for serialize and deserialize */
  private Serialization<RpcRequest> serialization;

  /** fast method mapping cache */
  private final MethodMapCache methodMapCache = new MethodMapCache();

  private final LocalServiceHolder serviceHolder;

  public HttpServiceProviderEndpoint(LocalServiceHolder serviceHolder) {
    this(serviceHolder, new JdkSerialization<>());
  }

  public HttpServiceProviderEndpoint(LocalServiceHolder serviceHolder, Serialization<RpcRequest> serialization) {
    this.serialization = serialization;
    this.serviceHolder = serviceHolder;
  }

  @Override
  public Object handleRequest(RequestContext request) throws Throwable {
    Serialization<RpcRequest> serialization = getSerialization();
    RpcResponse response = getResponse(request, serialization);
    serialization.serialize(response, request.getOutputStream());
    return NONE_RETURN_VALUE;
  }

  /**
   * @param context http request context
   * @param serialization serialize strategy
   * @return returns a {@link RpcResponse}
   */
  private RpcResponse getResponse(RequestContext context, Serialization<RpcRequest> serialization) {
    try {
      RpcRequest request = serialization.deserialize(context.getInputStream());
      Object service = serviceHolder.getService(request.getServiceName());
      if (service == null) {
        return RpcResponse.ofThrowable(new ServiceNotFoundException());
      }
      MethodInvoker invoker = methodMapCache.get(new MethodCacheKey(request), service);
      Object[] args = request.getArguments();
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

  private static final class MethodMapCache extends MapCache<MethodCacheKey, MethodInvoker, Object> {

    @Override
    protected MethodInvoker createValue(MethodCacheKey key, Object service) {
      Method methodToUse = getMethod(key, service);
      if (methodToUse == null) {
        return null;
      }
      return MethodInvoker.fromMethod(methodToUse);
    }

    private static Method getMethod(MethodCacheKey key, Object service) {
      String method = key.method;
      String[] paramTypes = key.paramTypes;
      int parameterLength = paramTypes.length;

      Class<Object> serviceImpl = ClassUtils.getUserClass(service);
      for (Method serviceMethod : serviceImpl.getMethods()) {
        if (Objects.equals(serviceMethod.getName(), method)
                && parameterLength == serviceMethod.getParameterCount()) {
          int current = 0;
          boolean equals = true;
          for (Class<?> parameterType : serviceMethod.getParameterTypes()) {
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
      if (!(o instanceof MethodCacheKey that))
        return false;
      return Objects.equals(method, that.method)
              && Arrays.equals(paramTypes, that.paramTypes);
    }

    @Override
    public int hashCode() {
      return 31 * Objects.hash(method) + Arrays.hashCode(paramTypes);
    }
  }
}
