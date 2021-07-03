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

package cn.taketoday.rpc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.Serializable;

import cn.taketoday.context.conversion.support.DefaultConversionService;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.rpc.utils.ObjectMapperUtils;

/**
 * @author TODAY 2021/7/4 01:19
 */
public class RpcRequest implements Serializable {

  private String serviceName;

  private String method;
  private String[] paramTypes;

  private String arguments;

  public void setArguments(String arguments) {
    this.arguments = arguments;
  }

  public void setArguments(Object[] arguments) {
    try {
      this.arguments = ObjectMapperUtils.toJSON(arguments);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public void setParamTypes(Class<?>[] parameterTypes) {
    String[] paramTypes = new String[parameterTypes.length];
    int i = 0;
    for (final Class<?> parameterType : parameterTypes) {
      paramTypes[i++] = parameterType.getName();
    }
    this.paramTypes = paramTypes;
  }

  public String[] getParamTypes() {
    return paramTypes;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getMethod() {
    return method;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getArguments() {
    return arguments;
  }

  public Object[] getArguments(Class<?>[] parameterTypes) throws IOException {
    Object[] args = new Object[parameterTypes.length];
    int i = 0;
    final ObjectMapper sharedMapper = ObjectMapperUtils.getSharedMapper();
    final JsonNode jsonNode = sharedMapper.readTree(arguments);
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
    return args;
  }
}
