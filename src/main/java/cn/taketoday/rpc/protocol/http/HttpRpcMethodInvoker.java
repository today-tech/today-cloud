/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.rpc.protocol.http;

import java.io.IOException;
import java.lang.reflect.Method;

import cn.taketoday.context.conversion.support.DefaultConversionService;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.rpc.HttpRpcRequest;
import cn.taketoday.rpc.RpcMethodInvoker;
import cn.taketoday.rpc.registry.ServiceDefinition;
import cn.taketoday.rpc.utils.HttpUtils;
import cn.taketoday.rpc.utils.ObjectMapperUtils;

/**
 * @author TODAY 2021/7/4 23:10
 */
public class HttpRpcMethodInvoker extends RpcMethodInvoker {

  @Override
  protected <T> Object doInvoke(ServiceDefinition definition, Method method, Object[] args) throws IOException {
    final HttpRpcRequest rpcRequest = new HttpRpcRequest();
    rpcRequest.setMethod(method.getName());
    rpcRequest.setServiceName(definition.getName());
    rpcRequest.setParamTypes(method.getParameterTypes());
    rpcRequest.setArguments(args);
    final String host = definition.getHost();
    final int port = definition.getPort();

    final String json = HttpUtils.doPost("http://" + host + ":" + port + "/provider", ObjectMapperUtils.toJSON(rpcRequest));

    final Class<?> returnType = method.getReturnType();
    if (ClassUtils.isSimpleType(returnType)) {
      return DefaultConversionService.getSharedInstance()
              .convert(json, returnType);
    }
    else
      return ObjectMapperUtils.fromJSON(json, returnType);
  }

}
