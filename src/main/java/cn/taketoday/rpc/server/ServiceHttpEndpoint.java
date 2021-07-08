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

import java.util.Map;

import cn.taketoday.context.reflect.MethodInvoker;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.rpc.HttpRpcRequest;
import cn.taketoday.rpc.RpcResponse;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * @author TODAY 2021/7/4 01:14
 */
@RequestMapping("/provider")
public class ServiceHttpEndpoint {
  final Map<String, Object> local;

  public ServiceHttpEndpoint(Map<String, Object> local) {
    this.local = local;
  }

  @POST
  public RpcResponse provider(@RequestBody HttpRpcRequest request) throws Exception {
    final Object service = local.get(request.getServiceName());
    final String method = request.getMethod();
    final String[] paramTypes = request.getParamTypes();
    final Class<?>[] parameterTypes = new Class<?>[paramTypes.length];

    int i = 0;
    for (final String paramType : paramTypes) {
      parameterTypes[i++] = Class.forName(paramType);
    }

    final Object[] args = request.resolveArguments(parameterTypes);
    final MethodInvoker invoker = getMethod(service, method, parameterTypes);
    return RpcResponse.of(invoker.invoke(service, args));
  }

  private MethodInvoker getMethod(Object service, String method, Class<?>[] parameterTypes) throws NoSuchMethodException {
    final Class<Object> serviceImpl = ClassUtils.getUserClass(service);
    return MethodInvoker.create(serviceImpl, method, parameterTypes);
  }

  @ExceptionHandler
  public void ex(Throwable throwable) {
    throwable.printStackTrace();
  }

}
