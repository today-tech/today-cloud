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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
public class HttpServiceRegistryEndpoint implements ServiceRegistry {
  private static final Logger log = LoggerFactory.getLogger(HttpServiceRegistryEndpoint.class);

  private final MultiValueMap<String, ServiceDefinition>
          serviceMapping = new DefaultMultiValueMap<>(new ConcurrentHashMap<>());

  @GET(produces = MediaType.APPLICATION_JSON_VALUE)
  public MultiValueMap<String, ServiceDefinition> services() {
    return serviceMapping;
  }

//  public ResponseEntity<MultiValueMap<String, ServiceDefinition>> services() {
//    return ResponseEntity.ok()
//            .contentType(MediaType.APPLICATION_JSON)
//            .body(serviceMapping);
//  }

  @GET("/{name}")
  @Override
  public List<ServiceDefinition> lookup(@PathVariable String name) {
    final List<ServiceDefinition> serviceDefinitions = serviceMapping.get(name);
    if (CollectionUtils.isEmpty(serviceDefinitions)) {
      throw new ServiceNotFoundException("cannot found a service: " + name);
    }
    // select one
    return serviceDefinitions;
  }

  @Override
  public List<ServiceDefinition> lookup(Class<?> serviceInterface) {
    return lookup(serviceInterface.getName());
  }

  @POST
  @Override
  public RegisteredStatus register(@RequestBody List<ServiceDefinition> definitions) {
    Logger log = HttpServiceRegistryEndpoint.log;
    MultiValueMap<String, ServiceDefinition> serviceMapping = getServiceMapping();
    for (final ServiceDefinition definition : definitions) {
      log.info("Registering service: [{}] ", definition);
      serviceMapping.add(definition.getName(), definition);
    }
    return RegisteredStatus.ofRegistered(definitions.size());
  }

  @DELETE
  @Override
  public void unregister(@RequestBody List<ServiceDefinition> definitions) {
    MultiValueMap<String, ServiceDefinition> serviceMapping = getServiceMapping();
    for (final ServiceDefinition definition : definitions) {
      final List<ServiceDefinition> serviceDefinitions = serviceMapping.get(definition.getName());
      if (!CollectionUtils.isEmpty(serviceDefinitions)
              && serviceDefinitions.removeIf(def -> Objects.equals(def, definition))) {
        log.info("un-register service: [{}] ", definition);
      }
    }
  }

  public MultiValueMap<String, ServiceDefinition> getServiceMapping() {
    return serviceMapping;
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(ServiceNotFoundException.class)
  void handleServiceNotFound(ServiceNotFoundException serviceNotFound) {

  }

}
