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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author TODAY 2021/7/4 01:19
 */
public class RpcRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  private String method;
  private String serviceName;
  private String[] paramTypes;

  public void setParamTypes(String[] paramTypes) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof RpcRequest))
      return false;
    final RpcRequest that = (RpcRequest) o;
    return Objects.equals(serviceName, that.serviceName)
            && Objects.equals(method, that.method)
            && Arrays.equals(paramTypes, that.paramTypes);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(serviceName, method);
    result = 31 * result + Arrays.hashCode(paramTypes);
    return result;
  }

  @Override
  public String toString() {
    return "RpcRequest{" +
            "serviceName='" + serviceName + '\'' +
            ", method='" + method + '\'' +
            ", paramTypes=" + Arrays.toString(paramTypes) +
            '}';
  }

  //

  public void setParamTypes(Class<?>[] parameterTypes) {
    String[] paramTypes = new String[parameterTypes.length];
    int i = 0;
    for (final Class<?> parameterType : parameterTypes) {
      paramTypes[i++] = parameterType.getName();
    }
    this.paramTypes = paramTypes;
  }

  public Object[] resolveArguments(Class<?>[] parameterTypes) {
    Object[] args = new Object[parameterTypes.length];
    resolveArgumentsInternal(args, parameterTypes);
    return args;
  }

  protected void resolveArgumentsInternal(Object[] args, Class<?>[] parameterTypes) {

  }

}
