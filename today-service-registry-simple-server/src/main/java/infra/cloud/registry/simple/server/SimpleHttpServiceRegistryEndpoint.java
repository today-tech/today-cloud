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

package infra.cloud.registry.simple.server;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import infra.cloud.registry.ServiceNotFoundException;
import infra.cloud.registry.simple.HttpRegistration;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.web.annotation.DELETE;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;

/**
 * @author TODAY 2021/7/9 23:08
 */
@RestController
@RequestMapping("${registry.services.uri:/services}")
class SimpleHttpServiceRegistryEndpoint implements SimpleHttpServiceRegistryAPI {

  private static final Logger log = LoggerFactory.getLogger(SimpleHttpServiceRegistryEndpoint.class);

  private final MultiValueMap<String, HttpRegistration> serviceMapping
          = MultiValueMap.forAdaption(new ConcurrentHashMap<>());

  @Override
  @GET(produces = MediaType.APPLICATION_JSON_VALUE)
  public MultiValueMap<String, HttpRegistration> services() {
    return serviceMapping;
  }

  @GET("/{name}")
  @Override
  public List<HttpRegistration> lookup(@PathVariable String name) {
    List<HttpRegistration> registrations = serviceMapping.get(name);
    if (CollectionUtils.isEmpty(registrations)) {
      throw new ServiceNotFoundException(name);
    }

    return registrations;
  }

  @POST
  @Override
  public void register(@RequestBody HttpRegistration registration) {
    log.info("Registering service: [{}] ", registration);
    serviceMapping.add(registration.getServiceId(), registration);
  }

  @PUT
  @Override
  public void update(HttpRegistration registration) {
    log.info("Updating service: [{}] ", registration);
    List<HttpRegistration> registrations = serviceMapping.get(registration.getServiceId());
    if (CollectionUtils.isEmpty(registrations)) {
      throw new ServiceNotFoundException(registration.getServiceId());
    }
    else {
      registrations.removeIf(r -> r.getInstanceId().equals(registration.getInstanceId()));
    }
  }

  @DELETE
  @Override
  public void unregister(@RequestBody HttpRegistration registration) {
    List<HttpRegistration> serviceDefinitions = serviceMapping.get(registration.getServiceId());
    if (CollectionUtils.isNotEmpty(serviceDefinitions)
            && serviceDefinitions.removeIf(def -> Objects.equals(def, registration))) {
      log.info("un-register service: [{}] ", registration);
    }
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(ServiceNotFoundException.class)
  void handleServiceNotFound(ServiceNotFoundException serviceNotFound) {

  }

}
