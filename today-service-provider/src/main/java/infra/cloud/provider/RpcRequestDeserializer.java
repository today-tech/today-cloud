/*
 * Copyright 2021 - 2024 the original author or authors.
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

package infra.cloud.provider;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import infra.cloud.RpcRequest;
import infra.cloud.serialize.RpcArgumentSerialization;
import infra.cloud.serialize.SerializationException;
import infra.lang.Nullable;
import infra.reflect.MethodInvoker;
import infra.util.ClassUtils;
import infra.util.MapCache;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/3/8 22:43
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RpcRequestDeserializer {

  private final List<RpcArgumentSerialization> argumentSerializations;

  /** fast method mapping cache */
  private final MethodMapCache methodMapCache = new MethodMapCache();

  public RpcRequestDeserializer(List<RpcArgumentSerialization> argumentSerializations) {
    this.argumentSerializations = argumentSerializations;
  }

  public RpcRequest deserialize(ByteBuf payload) throws SerializationException {
    RpcRequest rpcRequest = new RpcRequest();
//    ByteBufInput input = new ByteBufInput(payload);

    rpcRequest.setMethodName(payload.readCharSequence(payload.readInt(), StandardCharsets.UTF_8).toString());
    rpcRequest.setServiceName(payload.readCharSequence(payload.readInt(), StandardCharsets.UTF_8).toString());

    InvocableRpcMethod rpcMethod = methodMapCache.get(new MethodCacheKey(rpcRequest), null);

//    body.readInt();
//    body.readCharSequence();

    return rpcRequest;
  }

  private static final class MethodMapCache extends MapCache<MethodCacheKey, InvocableRpcMethod, Object> {

    @Nullable
    @Override
    protected InvocableRpcMethod createValue(MethodCacheKey key, @Nullable Object service) {
      Method methodToUse = getMethod(key, service);
      if (methodToUse == null) {
        return null;
      }
      MethodInvoker methodInvoker = MethodInvoker.forMethod(methodToUse);
      return new InvocableRpcMethod(methodToUse, methodInvoker);
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
      this.method = request.getMethodName();
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
