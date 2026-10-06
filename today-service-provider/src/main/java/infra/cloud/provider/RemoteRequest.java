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

package infra.cloud.provider;

import org.jspecify.annotations.Nullable;

import infra.cloud.service.Metadata;
import infra.core.DefaultAttributeAccessor;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/27 22:11
 */
public class RemoteRequest extends DefaultAttributeAccessor {

  private final InvocableMethod method;

  private final @Nullable Object @Nullable [] args;

  private final ServiceObject serviceObject;

  private final Metadata metadata;

  public RemoteRequest(InvocableMethod method, @Nullable Object @Nullable [] args, ServiceObject serviceObject) {
    this(method, args, serviceObject, Metadata.empty());
  }

  public RemoteRequest(InvocableMethod method, @Nullable Object @Nullable [] args, ServiceObject serviceObject,
          Metadata metadata) {
    this.method = method;
    this.args = args;
    this.serviceObject = serviceObject;
    this.metadata = metadata.snapshot();
  }

  /** Return immutable, buffer-independent cross-process metadata. */
  public Metadata getMetadata() {
    return metadata;
  }

  @Nullable
  public Object invoke() throws Throwable {
    return method.invoke(args);
  }

  public InvocableMethod getMethod() {
    return method;
  }

  public @Nullable Object @Nullable [] getArguments() {
    return args;
  }

  public ServiceObject getServiceObject() {
    return serviceObject;
  }

}
