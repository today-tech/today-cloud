/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.cloud;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import infra.cloud.serialize.Readable;
import infra.cloud.serialize.Message;
import infra.cloud.serialize.Writable;
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
  public void writeTo(Writable writable) {
    writable.write(serviceClass);
    writable.write(methodName);
    writable.write(paramTypes, Writable::write);
  }

  @Override
  public void readFrom(Readable readable) {
    this.serviceClass = readable.readString();
    this.methodName = readable.readString();
    this.paramTypes = readable.read(String.class, Readable::readString);
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
