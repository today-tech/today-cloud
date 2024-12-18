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

package infra.cloud.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import infra.cloud.DefaultServiceInstance;
import infra.cloud.ServiceInstance;
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
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;

/**
 * @author TODAY 2021/7/9 23:08
 */
@RestController
@RequestMapping("${registry.services.uri}")
public class HttpServiceRegistryEndpoint implements ServiceRegistry<HttpRegistration> {

  private static final Logger log = LoggerFactory.getLogger(HttpServiceRegistryEndpoint.class);

  private final MultiValueMap<String, ServiceDefinition> serviceMapping
          = MultiValueMap.forAdaption(new ConcurrentHashMap<>());

  @GET(produces = MediaType.APPLICATION_JSON_VALUE)
  public MultiValueMap<String, ServiceDefinition> services() {
    return serviceMapping;
  }

  @GET("/{name}")
  public List<ServiceInstance> lookup(@PathVariable String name) {
    List<ServiceDefinition> serviceDefinitions = serviceMapping.get(name);
    if (CollectionUtils.isEmpty(serviceDefinitions)) {
      throw new ServiceNotFoundException(name);
    }

    ArrayList<ServiceInstance> instances = new ArrayList<>();
    for (ServiceDefinition definition : serviceDefinitions) {
      var instance = new DefaultServiceInstance(definition.getHost() + ":" + definition.getPort(),
              definition.getName(), definition.getHost(), definition.getPort());
      instances.add(instance);
    }
    return instances;
  }

  @POST
  @Override
  public void register(@RequestBody HttpRegistration registration) {
    for (ServiceDefinition definition : registration.getServiceDefinitions()) {
      log.info("Registering service: [{}] ", definition);
      serviceMapping.add(definition.getName(), definition);
    }
  }

  @DELETE
  @Override
  public void unregister(@RequestBody HttpRegistration registration) {
    for (ServiceDefinition definition : registration.getServiceDefinitions()) {
      List<ServiceDefinition> serviceDefinitions = serviceMapping.get(definition.getName());
      if (CollectionUtils.isNotEmpty(serviceDefinitions)
              && serviceDefinitions.removeIf(def -> Objects.equals(def, definition))) {
        log.info("un-register service: [{}] ", definition);
      }
    }
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(ServiceNotFoundException.class)
  void handleServiceNotFound(ServiceNotFoundException serviceNotFound) {

  }

}
