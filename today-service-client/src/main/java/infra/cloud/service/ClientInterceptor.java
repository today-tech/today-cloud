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

/**
 * Intercepts calls on an interface on its way to the target. These are nested
 * "on top" of the target.
 *
 * <p>
 * The user should implement the {@link #intercept(ServiceInvocation)} method to
 * modify the original behavior. E.g. the following class implements a tracing
 * interceptor (traces all the calls on the intercepted method(s)):
 *
 * <pre>{@code
 * class TracingInterceptor implements ServiceInterceptor {
 *    public Object intercept(ServiceInvocation i) throws Throwable {
 *         System.out.println("before service " + i + " with args " + i.getArguments());
 *         Object ret = i.proceed();
 *         System.out.println("after service " + i + " returns " + ret);
 *         return ret;
 *     }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 15:39
 */
public interface ClientInterceptor {

  InvocationResult intercept(ServiceInvocation invocation) throws Throwable;

}
