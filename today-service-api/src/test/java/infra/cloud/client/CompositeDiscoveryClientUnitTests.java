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

package infra.cloud.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito tests for Composite Discovery Client
 *
 * @author Sean Ruffatti
 */
@ExtendWith(MockitoExtension.class)
class CompositeDiscoveryClientUnitTests {

  private CompositeDiscoveryClient underTest;

  @Mock
  private DiscoveryClient client1;

  @Mock
  private DiscoveryClient client2;

  @BeforeEach
  void setUp() {
    underTest = new CompositeDiscoveryClient(Arrays.asList(client1, client2));
  }

  @Test
  void shouldRetrieveInstancesByServiceId() {
    ServiceInstance serviceInstance1 = new DefaultServiceInstance("instance1", "serviceId", "https://s1", 8443,
            true);
    when(client1.getInstances("serviceId")).thenReturn(Collections.singletonList(serviceInstance1));

    List<ServiceInstance> serviceInstances = underTest.getInstances("serviceId");

    then(serviceInstances.get(0).getInstanceId()).isEqualTo("instance1");
    then(serviceInstances.get(0).getServiceId()).isEqualTo("serviceId");
    then(serviceInstances.get(0).getHost()).isEqualTo("https://s1");
    then(serviceInstances.get(0).getPort()).isEqualTo(8443);
  }

  @Test
  void shouldReturnServiceIds() {
    when(client1.getServices()).thenReturn(Collections.singletonList("serviceId1"));
    when(client2.getServices()).thenReturn(Collections.singletonList("serviceId2"));

    List<String> services = underTest.getServices();

    then(services.size()).isEqualTo(2);
    then(services).containsOnlyOnce("serviceId1", "serviceId2");
  }

  @Test
  void shouldReturnAllDiscoveryClients() {
    then(underTest.getDiscoveryClients()).containsOnlyOnce(client1, client2);
  }

  @Test
  void shouldCallProbeOnAllDiscoveryClients() {
    underTest.probe();

    // Every DiscoveryClient bean should invoke DiscoveryClient.probe() when
    // CompositeDiscoveryClient.probe() is invoked.
    verify(client1, times(1)).probe();
    verify(client1, times(0)).getServices();
    verify(client2, times(1)).probe();
    verify(client2, times(0)).getServices();
  }

}
