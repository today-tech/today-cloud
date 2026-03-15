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

package infra.cloud.registry.simple.server;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import infra.cloud.registry.simple.HttpRegistration;
import infra.cloud.registry.simple.api.SimpleHttpServiceRegistryAPI;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.lang.Assert;
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
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;

/**
 * @author TODAY 2021/7/9 23:08
 */
@RestController
class SimpleHttpServiceRegistryEndpoint implements SimpleHttpServiceRegistryAPI {

  private static final Logger log = LoggerFactory.getLogger(SimpleHttpServiceRegistryEndpoint.class);

  private final MultiValueMap<String, HttpRegistration> serviceMapping
          = MultiValueMap.forAdaption(new ConcurrentHashMap<>());

  @Override
  @GET(produces = MediaType.APPLICATION_JSON_VALUE)
  public MultiValueMap<String, HttpRegistration> services() {
    return serviceMapping;
  }

  @Override
  @GET("/{serviceId}")
  public List<HttpRegistration> lookup(@PathVariable String serviceId) {
    List<HttpRegistration> registrations = serviceMapping.get(serviceId);
    if (CollectionUtils.isEmpty(registrations)) {
      throw new ServiceNotFoundException(serviceId);
    }

    return registrations;
  }

  @POST
  @Override
  public void register(@RequestBody HttpRegistration registration) {
    Assert.notNull(registration.getServiceId(), "Service ID is required");
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
      registrations.add(registration);
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
