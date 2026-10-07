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

package infra.cloud.service;

import java.lang.reflect.Type;

import infra.core.MethodParameter;

/**
 * Parameter view used by argument codecs for a stream's element, retaining the
 * original parameter annotations without mutating cached method metadata.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/7
 */
public final class StreamElementParameter extends MethodParameter {

  private final MethodParameter element;

  private StreamElementParameter(MethodParameter parameter) {
    super(parameter);
    element = parameter.nested();
  }

  /**
   * Create an element view for a parameter with a concrete generic element type.
   */
  public static MethodParameter of(MethodParameter parameter) {
    StreamElementParameter result = new StreamElementParameter(parameter);
    if (result.getParameterType() == Object.class) {
      throw new IllegalArgumentException("Channel input requires a concrete element type");
    }
    return result;
  }

  @Override
  public Class<?> getParameterType() {
    return element.getNestedParameterType();
  }

  @Override
  public Type getGenericParameterType() {
    return element.getNestedGenericParameterType();
  }
}
