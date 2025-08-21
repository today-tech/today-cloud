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

import infra.cloud.serialize.Input;
import infra.cloud.serialize.Message;
import infra.cloud.serialize.Output;
import infra.cloud.service.ServiceMethod;

/**
 * @author TODAY 2021/7/4 01:19
 */
public class RpcRequest implements Serializable, Message {

  @Serial
  private static final long serialVersionUID = 1L;

  private String serviceClass;

  private String methodName;

  private Object[] arguments;

  private String[] paramTypes;

  private ServiceMethod method;

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

  public void setMethod(ServiceMethod method) {
    this.method = method;
  }

  public ServiceMethod getMethod() {
    return method;
  }

  public void setServiceClass(String serviceName) {
    this.serviceClass = serviceName;
  }

  public String getServiceClass() {
    return serviceClass;
  }

  @Override
  public void writeTo(Output output) {
    output.write(serviceClass);
    output.write(methodName);
    output.write(paramTypes, Output::write);
  }

  @Override
  public void readFrom(Input input) {
    this.serviceClass = input.readString();
    this.methodName = input.readString();
    this.paramTypes = input.read(String.class, Input::readString);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof RpcRequest request))
      return false;
    return Objects.equals(methodName, request.methodName)
            && Objects.equals(serviceClass, request.serviceClass)
            && Arrays.equals(paramTypes, request.paramTypes)
            && Arrays.equals(arguments, request.arguments);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(methodName, serviceClass);
    result = 31 * result + Arrays.hashCode(paramTypes);
    result = 31 * result + Arrays.hashCode(arguments);
    return result;
  }

  @Override
  public String toString() {
    return "RpcRequest{" +
            "method='" + methodName + '\'' +
            ", serviceName='" + serviceClass + '\'' +
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
