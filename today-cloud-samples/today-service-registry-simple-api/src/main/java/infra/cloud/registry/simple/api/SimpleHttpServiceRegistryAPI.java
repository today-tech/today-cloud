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

package infra.cloud.registry.simple.api;

import java.util.List;

import infra.cloud.registry.simple.HttpRegistration;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;

/**
 * HTTP API interface for simple service registry operations.
 * Provides methods for registering, unregistering, updating and looking up services.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/8 11:18
 */
@RequestMapping("${registry.services.uri:/services}")
public interface SimpleHttpServiceRegistryAPI {

  @GET(produces = MediaType.APPLICATION_JSON_VALUE)
  MultiValueMap<String, HttpRegistration> services();

  @GET("/{serviceId}")
  List<HttpRegistration> lookup(@PathVariable String serviceId);

  @POST
  void register(@RequestBody HttpRegistration registration);

  @DELETE
  void unregister(@RequestBody HttpRegistration registration);

  @PUT
  void update(@RequestBody HttpRegistration registration);

}
