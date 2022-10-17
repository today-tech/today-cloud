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

package cn.taketoday.cloud.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import cn.taketoday.cloud.RpcRequest;
import cn.taketoday.cloud.RpcResponse;
import cn.taketoday.cloud.ServiceMethodInvoker;
import cn.taketoday.cloud.core.HttpUtils;
import cn.taketoday.cloud.core.serialize.Serialization;
import cn.taketoday.cloud.registry.ServiceDefinition;

/**
 * @author TODAY 2021/7/4 23:10
 */
public class HttpServiceMethodInvoker extends ServiceMethodInvoker {

  public HttpServiceMethodInvoker(Serialization<RpcResponse> serialization) {
    super(serialization);
  }

  @Override
  protected RpcResponse invokeInternal(ServiceDefinition selected, Method method, Object[] args)
          throws IOException, ClassNotFoundException {
    final RpcRequest rpcRequest = new RpcRequest();
    rpcRequest.setMethod(method.getName());
    rpcRequest.setServiceName(selected.getName());
    rpcRequest.setParameterTypes(method.getParameterTypes());
    rpcRequest.setArguments(args);

    final Serialization<RpcResponse> serialization = getSerialization();
    final InputStream inputStream = HttpUtils.postInputStream(
            buildServiceProviderURL(selected),
            output -> serialization.serialize(rpcRequest, output)
    );
    return serialization.deserialize(inputStream);
  }

  protected String buildServiceProviderURL(ServiceDefinition definition) {
    final String host = definition.getHost();
    final int port = definition.getPort();
    return "http://" + host + ':' + port + "/provider";
  }

}
