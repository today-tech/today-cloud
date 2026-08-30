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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodIdGenerator {

  public static Map<Method, Integer> generateMethodIds(Set<Class<?>> serviceInterfaces) {
    List<Method> allMethods = new ArrayList<>();
    for (Class<?> serviceInterface : serviceInterfaces) {
      collectMethods(serviceInterface, allMethods);
    }

    allMethods.sort((m1, m2) -> {
      int cmp = m1.getDeclaringClass().getName().compareTo(m2.getDeclaringClass().getName());
      if (cmp != 0)
        return cmp;

      cmp = m1.getName().compareTo(m2.getName());
      if (cmp != 0)
        return cmp;

      Class<?>[] params1 = m1.getParameterTypes();
      Class<?>[] params2 = m2.getParameterTypes();
      cmp = Integer.compare(params1.length, params2.length);
      if (cmp != 0)
        return cmp;

      for (int i = 0; i < params1.length; i++) {
        cmp = params1[i].getName().compareTo(params2[i].getName());
        if (cmp != 0)
          return cmp;
      }
      return 0;
    });

    Map<Method, Integer> methodIds = new HashMap<>();
    int id = 1;
    for (Method method : allMethods) {
      methodIds.put(method, id++);
    }
    return methodIds;
  }

  private static void collectMethods(Class<?> interfaceClass, List<Method> methods) {
    methods.addAll(Arrays.asList(interfaceClass.getDeclaredMethods()));

    for (Class<?> parent : interfaceClass.getInterfaces()) {
      collectMethods(parent, methods);
    }
  }

}