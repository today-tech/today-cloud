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

package infra.cloud.client.annotation.config;

import java.util.List;

import infra.beans.factory.ObjectProvider;
import infra.cloud.client.DiscoveryClient;
import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.client.simple.SimpleDiscoveryProperties;
import infra.cloud.serialize.ArgumentSerialization;
import infra.cloud.serialize.ReturnValueDeserializer;
import infra.cloud.serialize.ThrowableSerialization;
import infra.cloud.service.ClientInterceptor;
import infra.cloud.service.DefaultRemotingOperationsProvider;
import infra.cloud.service.DefaultServiceInterfaceMetadataProvider;
import infra.cloud.service.DefaultServiceMetadataProvider;
import infra.cloud.service.DefaultServiceProxyFactory;
import infra.cloud.service.RemotingOperationsProvider;
import infra.cloud.service.ReturnValueResolver;
import infra.cloud.service.ServiceInterfaceMetadataProvider;
import infra.cloud.service.ServiceInterfaceMethod;
import infra.cloud.service.ServiceInvoker;
import infra.cloud.service.ServiceMetadataProvider;
import infra.cloud.service.ServiceMethodInvoker;
import infra.cloud.service.serialize.RequestSerializer;
import infra.cloud.service.serialize.ResponseDeserializer;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.io.ResourceLoader;
import infra.stereotype.Component;
import infra.util.TodayStrategies;
import io.netty.buffer.ByteBufAllocator;

/**
 * Auto-configuration for remote service client.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 22:14
 */
@SuppressWarnings("rawtypes")
@DisableDIAutoConfiguration
@ConditionalOnDiscoveryEnabled
@EnableConfigurationProperties(SimpleDiscoveryProperties.class)
public final class ServiceClientAutoConfiguration {

  @Component
  public static DefaultServiceProxyFactory serviceProxyFactory(
          ServiceInterfaceMetadataProvider<ServiceInterfaceMethod> metadataProvider, ServiceInvoker serviceInvoker) {
    return new DefaultServiceProxyFactory(metadataProvider, serviceInvoker);
  }

  @Component
  public static ServiceInvoker serviceInvoker(List<ClientInterceptor> interceptors, RemotingOperationsProvider remotingOperationsProvider,
          RequestSerializer requestSerializer, ResponseDeserializer responseDeserializer) {
    return new ServiceMethodInvoker(interceptors, remotingOperationsProvider, ByteBufAllocator.DEFAULT,
            requestSerializer, responseDeserializer);
  }

  @Component
  public static ThrowableSerialization throwableSerialization() {
    return new ThrowableSerialization();
  }

  @Component
  public static ResponseDeserializer responseDeserializer(List<ReturnValueDeserializer> serializations,
          ThrowableSerialization throwableSerialization, ResourceLoader resourceLoader) {
    // order after ReturnValueDeserializer beans
    serializations.addAll(TodayStrategies.find(ReturnValueDeserializer.class, resourceLoader.getClassLoader()));
    return new ResponseDeserializer(serializations, throwableSerialization);
  }

  @Component
  public static RequestSerializer requestSerializer(List<ArgumentSerialization> argumentSerializations, ResourceLoader resourceLoader) {
    List<ArgumentSerialization> serializations = TodayStrategies.find(ArgumentSerialization.class, resourceLoader.getClassLoader());
    argumentSerializations.addAll(serializations); // order after ArgumentSerialization beans
    return new RequestSerializer(argumentSerializations);
  }

  @Component
  @ConditionalOnMissingBean
  public static RemotingOperationsProvider remotingOperationsProvider(DiscoveryClient discoveryClient) {
    return new DefaultRemotingOperationsProvider(discoveryClient);
  }

  @Component
  @ConditionalOnMissingBean
  public static ServiceMetadataProvider serviceMetadataProvider() {
    return new DefaultServiceMetadataProvider();
  }

  @Component
  @ConditionalOnMissingBean
  public static ServiceInterfaceMetadataProvider<ServiceInterfaceMethod> serviceInterfaceMetadataProvider(
          ServiceMetadataProvider serviceMetadataProvider, ObjectProvider<ReturnValueResolver> resolvers) {
    return new DefaultServiceInterfaceMetadataProvider(serviceMetadataProvider, resolvers.orderedList());
  }

}
