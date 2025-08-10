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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/8 11:18
 */
@RequestMapping("${registry.services.uri:/services}")
public interface SimpleHttpServiceRegistryAPI {

  @GET(produces = MediaType.APPLICATION_JSON_VALUE)
  MultiValueMap<String, HttpRegistration> services();

  @GET("/{name}")
  List<HttpRegistration> lookup(@PathVariable String name);

  @POST
  void register(@RequestBody HttpRegistration registration);

  @DELETE
  void unregister(@RequestBody HttpRegistration registration);

  @PUT
  void update(@RequestBody HttpRegistration registration);

}
