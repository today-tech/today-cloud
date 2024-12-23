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

package infra.cloud;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import infra.cloud.registry.InstanceSelector;
import infra.cloud.registry.RandomInstanceSelector;
import infra.lang.Assert;
import infra.util.concurrent.Future;
import reactor.core.publisher.Mono;

/**
 * Service Method Invoker
 *
 * @author TODAY 2021/7/4 01:58
 */
public abstract class ServiceMethodInvoker {

  protected InstanceSelector instanceSelector = new RandomInstanceSelector();

  protected RemoteExceptionHandler exceptionHandler = new SimpleRemoteExceptionHandler();

  private final ArrayList<ReturnValueResolver> resolvers = new ArrayList<>();

  public ServiceMethodInvoker() {
    resolvers.add(new ListenableFutureReturnValueResolver());
    resolvers.add(new MonoFutureReturnValueResolver());
    resolvers.add(new BlockFutureReturnValueResolver());
  }

  public Object invoke(List<ServiceInstance> instances, Method method, Object[] args) throws Throwable {
    ServiceInstance selected = instanceSelector.select(instances);
    try {
      Future<Object> response = invokeInternal(selected, method, args);
      return resolveReturnValue(response, method);
    }
    catch (Throwable e) {
      return handleException(e, instances, selected, method, args);
    }
  }

  private Object resolveReturnValue(Future<Object> response, Method method) throws Throwable {
    for (ReturnValueResolver resolver : resolvers) {
      if (resolver.supports(method)) {
        return resolver.resolve(response, method);
      }
    }
    return null;
  }

  protected RpcResponse handleException(Throwable e, List<ServiceInstance> instances,
          ServiceInstance selected, Method method, Object[] args) throws Throwable {

    throw e;
  }

  protected abstract Future<Object> invokeInternal(ServiceInstance selected, Method method, Object[] args)
          throws Throwable;

  public void setExceptionHandler(RemoteExceptionHandler exceptionHandler) {
    Assert.notNull(exceptionHandler, "exceptionHandler is required");
    this.exceptionHandler = exceptionHandler;
  }

  public RemoteExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  public void setServiceSelector(InstanceSelector instanceSelector) {
    Assert.notNull(instanceSelector, "serviceSelector is required");
    this.instanceSelector = instanceSelector;
  }

  public InstanceSelector getServiceSelector() {
    return instanceSelector;
  }

  interface ReturnValueResolver {

    boolean supports(Method method);

    Object resolve(Future<Object> response, Method method) throws Throwable;

  }

  static class ListenableFutureReturnValueResolver implements ReturnValueResolver {

    @Override
    public boolean supports(Method method) {
      return method.getReturnType() == Future.class;
    }

    @Override
    public Object resolve(Future<Object> response, Method method) {
      return response;
    }

  }

  static class BlockFutureReturnValueResolver implements ReturnValueResolver {

    @Override
    public boolean supports(Method method) {
      return true;
    }

    @Override
    public Object resolve(Future<Object> response, Method method) {
      response.syncUninterruptibly();
      return response.getNow();
    }

  }

  static class MonoFutureReturnValueResolver implements ReturnValueResolver {

    @Override
    public boolean supports(Method method) {
      return method.getReturnType() == Mono.class;
    }

    @Override
    public Object resolve(Future<Object> response, Method method) {
      return Mono.fromFuture(response.completable());
    }

  }

}
