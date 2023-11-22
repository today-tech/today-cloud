/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.cloud.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.cloud.DefaultServiceInstance;
import cn.taketoday.cloud.ServiceInstance;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.DefaultMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;

/**
 * @author TODAY 2021/7/9 23:08
 */
@RestController
@RequestMapping("${registry.services.uri}")
public class HttpServiceRegistryEndpoint implements ServiceRegistry<HttpRegistration> {

  private static final Logger log = LoggerFactory.getLogger(HttpServiceRegistryEndpoint.class);

  private final MultiValueMap<String, ServiceDefinition>
          serviceMapping = new DefaultMultiValueMap<>(new ConcurrentHashMap<>());

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
