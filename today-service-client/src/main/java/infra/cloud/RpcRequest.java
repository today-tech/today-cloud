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

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author TODAY 2021/7/4 01:19
 */
public class RpcRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String methodName;

  private String serviceName;

  private Object[] arguments;

  private String[] paramTypes;

  private RpcMethod rpcMethod;

  public void setArguments(Object[] arguments) {
    this.arguments = arguments;
  }

  public Object[] getArguments() {
    return arguments;
  }

  public void setParamTypes(String[] paramTypes) {
    this.paramTypes = paramTypes;
  }

  public String[] getParamTypes() {
    return paramTypes;
  }

  public void setMethodName(String method) {
    this.methodName = method;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setRpcMethod(RpcMethod rpcMethod) {
    this.rpcMethod = rpcMethod;
  }

  public RpcMethod getRpcMethod() {
    return rpcMethod;
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
    if (!(o instanceof RpcRequest request))
      return false;
    return Objects.equals(methodName, request.methodName)
            && Objects.equals(serviceName, request.serviceName)
            && Arrays.equals(paramTypes, request.paramTypes)
            && Arrays.equals(arguments, request.arguments);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(methodName, serviceName);
    result = 31 * result + Arrays.hashCode(paramTypes);
    result = 31 * result + Arrays.hashCode(arguments);
    return result;
  }

  @Override
  public String toString() {
    return "RpcRequest{" +
            "method='" + methodName + '\'' +
            ", serviceName='" + serviceName + '\'' +
            ", paramTypes=" + Arrays.toString(paramTypes) +
            ", arguments=" + Arrays.toString(arguments) +
            '}';
  }

  //

  public void setParameterTypes(Class<?>[] parameterTypes) {
    String[] paramTypes = new String[parameterTypes.length];
    int i = 0;
    for (final Class<?> parameterType : parameterTypes) {
      paramTypes[i++] = parameterType.getName();
    }
    this.paramTypes = paramTypes;
  }

}
