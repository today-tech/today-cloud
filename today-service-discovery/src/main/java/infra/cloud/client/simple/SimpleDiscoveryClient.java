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

package infra.cloud.client.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.cloud.client.DiscoveryClient;
import infra.cloud.client.ServiceInstance;

/**
 * A {@link infra.cloud.client.DiscoveryClient} that will use the
 * properties file as a source of service instances.
 *
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 * @author Charu Covindane
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class SimpleDiscoveryClient implements DiscoveryClient {

  private final SimpleDiscoveryProperties properties;

  public SimpleDiscoveryClient(SimpleDiscoveryProperties properties) {
    this.properties = properties;
  }

  @Override
  public String getDescription() {
    return "Simple Discovery Client";
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<ServiceInstance> getInstances(String serviceId) {
    List instances = properties.getInstances().get(serviceId);
    if (instances != null) {
      return Collections.unmodifiableList(instances);
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> getServices() {
    return new ArrayList<>(properties.getInstances().keySet());
  }

  @Override
  public int getOrder() {
    return properties.getOrder();
  }

}
