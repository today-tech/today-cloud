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

package cn.taketoday.cloud.provider;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.DeserializeFailedException;
import cn.taketoday.cloud.core.serialize.ProtostuffUtils;
import cn.taketoday.cloud.protocol.EventHandler;
import cn.taketoday.cloud.protocol.ProtocolPayload;
import cn.taketoday.cloud.protocol.RemoteEventType;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.MethodInvoker;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.MapCache;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/8 22:51
 */
public class RpcEventHandler implements EventHandler {

  /** fast method mapping cache */
  private final MethodMapCache methodMapCache = new MethodMapCache();

  private final LocalServiceHolder serviceHolder;

  private final RpcRequestDeserializer deserializer = new RpcRequestDeserializer();

  RpcEventHandler(LocalServiceHolder serviceHolder) {
    this.serviceHolder = serviceHolder;
  }

  @Override
  public Set<RemoteEventType> getSupportedEvents() {
    return Set.of(RemoteEventType.RPC_INVOCATION);
  }

  @Override
  public void handleEvent(Channel channel, ProtocolPayload payload) throws Exception {
    RpcResponse response = handle0(channel, payload);
    byte[] body = serialize(response);

    ByteBuf buffer = channel.alloc().buffer(4 + body.length + ProtocolPayload.HEADER_LENGTH);
    buffer.writeInt(body.length + ProtocolPayload.HEADER_LENGTH);
    payload.header.serialize(buffer);
    buffer.writeBytes(body);

    channel.writeAndFlush(buffer);
  }

  private byte[] serialize(RpcResponse response) throws IOException {
    return ProtostuffUtils.serialize(response);
  }

  protected RpcResponse handle0(Channel ctx, ProtocolPayload payload) throws IOException {
    ByteBuf body = payload.body;
    try {
      if (body != null) {
        RpcRequest request = deserializer.deserialize(body);
        Object service = serviceHolder.getService(request.getServiceName());
        if (service == null) {
          return RpcResponse.ofThrowable(new ServiceNotFoundException(request.getServiceName()));
        }
        MethodInvoker invoker = methodMapCache.get(new MethodCacheKey(request), service);
        Object[] args = request.getArguments();
        return createResponse(service, args, invoker);
      }
      return RpcResponse.empty;
    }
    catch (IOException io) {
      return RpcResponse.ofThrowable(new DeserializeFailedException(io));
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

  private static final class MethodMapCache extends MapCache<MethodCacheKey, MethodInvoker, Object> {

    @Override
    protected MethodInvoker createValue(MethodCacheKey key, @Nullable Object service) {
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
