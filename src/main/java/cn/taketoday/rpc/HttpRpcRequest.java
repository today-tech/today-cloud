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

package cn.taketoday.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import cn.taketoday.context.conversion.support.DefaultConversionService;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.rpc.server.MethodArgumentException;
import cn.taketoday.rpc.utils.ObjectMapperUtils;

/**
 * use http
 *
 * @author TODAY 2021/7/4 22:36
 */
public class HttpRpcRequest extends RpcRequest {

  private String arguments;

  public void setArguments(Object[] arguments) {
    try {
      this.arguments = ObjectMapperUtils.toJSON(arguments);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void resolveArgumentsInternal(Object[] args, Class<?>[] parameterTypes) {
    try {
      final ObjectMapper sharedMapper = ObjectMapperUtils.getSharedMapper();
      final JsonNode jsonNode = sharedMapper.readTree(arguments);
      int i = 0;
      for (final Class<?> parameterType : parameterTypes) {
        final JsonNode current = jsonNode.get(i);
        if (ClassUtils.isSimpleType(parameterType)) {
          args[i++] = DefaultConversionService.getSharedInstance().convert(current.asText(), parameterType);
        }
        else {
          final Object fromJSON = sharedMapper.readValue(current.asText(), parameterType);
          args[i++] = fromJSON;
        }
      }
    }
    catch (IOException e) {
      throw new MethodArgumentException(e);
    }
  }
}
