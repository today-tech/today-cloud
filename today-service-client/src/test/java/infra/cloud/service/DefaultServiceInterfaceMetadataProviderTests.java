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

package infra.cloud.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import infra.core.ResolvableType;
import infra.util.concurrent.Future;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 16:56
 */
class DefaultServiceInterfaceMetadataProviderTests {

  @Test
  void getMetadata() {

    ServiceMetadataProvider serviceMetadataProvider = serviceInterface -> {
      return new ServiceMetadata("demo-user-service", "1.0",
              List.of("infra.cloud.service.DefaultServiceInterfaceMetadataProviderTests.DemoUserService"));
    };

    var metadataProvider = new DefaultServiceInterfaceMetadataProvider(serviceMetadataProvider, List.of());

    var metadata = metadataProvider.getMetadata(DemoUserService.class);
    List<ServiceInterfaceMethod> serviceMethods = metadata.getServiceMethods();

    assertThat(metadata.getServiceMetadata().getVersion()).isEqualTo("1.0");
    assertThat(metadata.getServiceMetadata().getId()).isEqualTo("demo-user-service");
    assertThat(metadata.getServiceInterface()).isEqualTo(DemoUserService.class);

    assertThat(serviceMethods).hasSize(4);
    assertThatThrownBy(() -> serviceMethods.remove(1))
            .isInstanceOf(UnsupportedOperationException.class);

    Map<String, ServiceInterfaceMethod> methodMap = serviceMethods.stream().collect(Collectors.toMap(me -> me.getMethod().getName(), me -> me));

    ServiceInterfaceMethod getById = methodMap.get("getById");
    assertThat(getById.getServiceInterface()).isSameAs(metadata.getServiceInterface());
    assertThat(getById.getInvocationType()).isEqualTo(InvocationType.REQUEST_RESPONSE);
    assertThat(getById.getParameters().length).isEqualTo(1);
    assertThat(getById.getReturnType().getParameterType()).isEqualTo(User.class);

    ServiceInterfaceMethod listUsers = methodMap.get("listUsers");
    assertThat(listUsers.getInvocationType()).isEqualTo(InvocationType.REQUEST_RESPONSE);
    assertThat(listUsers.getParameters().length).isEqualTo(0);
    assertThat(listUsers.getReturnType().getParameterType()).isEqualTo(List.class);
    assertThat(ResolvableType.forMethodParameter(listUsers.getReturnType()).getGeneric().resolve()).isEqualTo(User.class);

    ServiceInterfaceMethod listUsersFuture = methodMap.get("listUsersFuture");
    assertThat(listUsersFuture.getInvocationType()).isEqualTo(InvocationType.REQUEST_RESPONSE);
    assertThat(listUsersFuture.getParameters().length).isEqualTo(0);
    assertThat(listUsersFuture.getReturnType().getParameterType()).isEqualTo(Future.class);

    assertThat(listUsersFuture.getInvocationType().clientSendsOneMessage()).isTrue();
    assertThat(listUsersFuture.getInvocationType().serverSendsOneMessage()).isTrue();

    ResolvableType resolvableType = ResolvableType.forMethodParameter(listUsersFuture.getReturnType());
    assertThat(resolvableType.getGeneric().resolve()).isEqualTo(List.class);
    assertThat(resolvableType.getGeneric().getGeneric().resolve()).isEqualTo(User.class);
    //

    ServiceInterfaceMethod listUsersFlux = methodMap.get("listUsersFlux");
    assertThat(listUsersFlux.getInvocationType()).isEqualTo(InvocationType.RESPONSE_STREAMING);
    assertThat(listUsersFlux.getParameters().length).isEqualTo(0);
    assertThat(listUsersFlux.getReturnType().getParameterType()).isEqualTo(Flux.class);
    assertThat(ResolvableType.forMethodParameter(listUsers.getReturnType()).getGeneric().resolve()).isEqualTo(User.class);

    assertThat(listUsersFlux.getInvocationType().clientSendsOneMessage()).isTrue();
    assertThat(listUsersFlux.getInvocationType().serverSendsOneMessage()).isFalse();

  }

  interface DemoUserService {

    User getById(int id);

    List<User> listUsers();

    Future<List<User>> listUsersFuture();

    Flux<User> listUsersFlux();
  }

  static class User {

  }

}