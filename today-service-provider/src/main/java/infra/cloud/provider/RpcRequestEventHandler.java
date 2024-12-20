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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import infra.cloud.RpcRequest;
import infra.cloud.RpcResponse;
import infra.cloud.protocol.ByteBufOutput;
import infra.cloud.protocol.Connection;
import infra.cloud.protocol.EventHandler;
import infra.cloud.protocol.ProtocolPayload;
import infra.cloud.protocol.RemoteEventType;
import infra.cloud.registry.ServiceNotFoundException;
import infra.cloud.serialize.RpcRequestSerialization;
import infra.cloud.serialize.RpcResponseSerialization;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.reflect.MethodInvoker;
import infra.util.ClassUtils;
import infra.util.MapCache;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/8 22:51
 */
public class RpcRequestEventHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(RpcRequestEventHandler.class);

  /** fast method mapping cache */
  private final MethodMapCache methodMapCache = new MethodMapCache();

  private final LocalServiceHolder serviceHolder;

  private final RpcRequestSerialization requestSerialization;

  private final RpcResponseSerialization responseSerialization;

  // todo configurable
  private final int initialCapacity = 64;

  RpcRequestEventHandler(LocalServiceHolder serviceHolder, RpcRequestSerialization requestSerialization,
          RpcResponseSerialization responseSerialization) {
    this.serviceHolder = serviceHolder;
    this.requestSerialization = requestSerialization;
    this.responseSerialization = responseSerialization;
  }

  @Override
  public Set<RemoteEventType> getSupportedEvents() {
    return Set.of(RemoteEventType.RPC_REQUEST);
  }

  @Override
  public void channelActive(Connection connection) {
    logger.debug("Client connected: [{}]", connection);
  }

  @Override
  public void handleEvent(Connection connection, ProtocolPayload payload) throws IOException {
    RpcResponse response = rpcInvoke(connection, payload);

    ByteBuf body = connection.alloc().buffer(initialCapacity);
    responseSerialization.serialize(response, body, new ByteBufOutput(body));

    connection.writeAndFlush(new ProtocolPayload(payload.header, body));
  }

  protected RpcResponse rpcInvoke(Connection connection, ProtocolPayload payload) {
    ByteBuf body = payload.body;
    if (body != null) {
      RpcRequest request = requestSerialization.deserialize(body);
      Object service = serviceHolder.getService(request.getServiceName());
      if (service == null) {
        return RpcResponse.ofThrowable(new ServiceNotFoundException(request.getServiceName()));
      }

      InvocableRpcMethod rpcMethod = methodMapCache.get(new MethodCacheKey(request), null);
      if (rpcMethod == null) {
        return RpcResponse.ofThrowable(new ServiceNotFoundException());
      }
      try {
        return new RpcResponse(rpcMethod.invokeAndHandle(service, request.getArguments()));
      }
      catch (Throwable e) {
        return RpcResponse.ofThrowable(e);
      }
    }
    return RpcResponse.empty;
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
