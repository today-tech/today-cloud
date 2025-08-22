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
      return new ServiceMetadata("demo-user-service", "1.0");
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