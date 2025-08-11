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