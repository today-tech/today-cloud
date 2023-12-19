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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.core.serialize.DeserializeFailedException;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.netty.ChannelConnector;
import cn.taketoday.cloud.registry.ServiceNotFoundException;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.MethodInvoker;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.MapCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/22 22:35
 */
public class ServiceProviderChannelConnector extends ChannelConnector implements SmartLifecycle {

  private final LocalServiceHolder serviceHolder;

  /** for serialize and deserialize */
  private final Serialization<RpcRequest> serialization;

  /** fast method mapping cache */
  private final MethodMapCache methodMapCache = new MethodMapCache();

  private volatile boolean running;

  public ServiceProviderChannelConnector(LocalServiceHolder serviceHolder, Serialization<RpcRequest> serialization) {
    this.serialization = serialization;
    this.serviceHolder = serviceHolder;
    setPort(serviceHolder.getPort());
  }

  @Override
  protected void onActive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("Client connected");
  }

  @Override
  protected void onInactive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("Client disconnected");
  }

  @Override
  protected void initChannel(Channel ch, ChannelPipeline pipeline) throws Exception {
    pipeline.addLast("tcp-frame-decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,
            0, 4, 0, 4));
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf byteBuf) {
      RpcResponse response = handle(ctx, byteBuf);
      ByteArrayOutputStream output = new ByteArrayOutputStream(128);
      serialization.serialize(response, output);

      byte[] writeBuffer = new byte[4];
      byte[] body = output.toByteArray();
      int length = body.length;

      writeBuffer[0] = (byte) (length >>> 24);
      writeBuffer[1] = (byte) (length >>> 16);
      writeBuffer[2] = (byte) (length >>> 8);
      writeBuffer[3] = (byte) (length >>> 0);

      ctx.writeAndFlush(Unpooled.wrappedBuffer(writeBuffer, body));
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  protected RpcResponse handle(ChannelHandlerContext ctx, ByteBuf byteBuf) throws IOException {
    try (ByteBufInputStream inputStream = new ByteBufInputStream(byteBuf)) {
      try {
        RpcRequest request = serialization.deserialize(inputStream);
        Object service = serviceHolder.getService(request.getServiceName());
        if (service == null) {
          return RpcResponse.ofThrowable(new ServiceNotFoundException(request.getServiceName()));
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

  @Override
  public void start() {
    bind();
    running = true;
  }

  @Override
  public void stop() {
    shutdown();
    running = false;
  }

  @Override
  public boolean isRunning() {
    return running;
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
