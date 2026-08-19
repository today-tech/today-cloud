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

package infra.cloud.provider.config;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.net.InetProperties;
import infra.cloud.net.InetService;
import infra.cloud.provider.DefaultServiceInterfaceMetadataProvider;
import infra.cloud.provider.LocalServiceHolder;
import infra.cloud.provider.RequestDeserializer;
import infra.cloud.provider.ResponseSerializer;
import infra.cloud.provider.ServerTransportFactory;
import infra.cloud.provider.ServiceChannelHandler;
import infra.cloud.provider.ServiceProviderServer;
import infra.cloud.provider.ServiceServerProperties;
import infra.cloud.provider.TcpServerTransportFactory;
import infra.cloud.serialize.ArgumentSerialization;
import infra.cloud.serialize.ReturnValueSerializer;
import infra.cloud.service.DefaultServiceMetadataProvider;
import infra.cloud.service.ServiceInterfaceMetadataProvider;
import infra.cloud.service.ServiceMetadataProvider;
import infra.cloud.service.ServiceMethod;
import infra.cloud.service.config.ResumeProperties;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnBooleanProperty;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.io.ResourceLoader;
import infra.remoting.Closeable;
import infra.remoting.core.Resume;
import infra.remoting.resume.InMemoryResumableFramesStoreFactory;
import infra.remoting.resume.RandomUUIDResumeTokenGenerator;
import infra.remoting.resume.ResumableFramesStoreFactory;
import infra.remoting.resume.ResumeTokenGenerator;
import infra.stereotype.Component;
import infra.util.TodayStrategies;

/**
 * Auto-configuration for the Service Provider.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 22:31
 */
@SuppressWarnings("rawtypes")
@DisableDIAutoConfiguration
@ConditionalOnDiscoveryEnabled
@EnableConfigurationProperties({ InetProperties.class, ServiceServerProperties.class })
public final class ServiceProviderAutoConfiguration {

  @Component
  public static LocalServiceHolder localServiceHolder(ServiceMetadataProvider metadataProvider) {
    return new LocalServiceHolder(metadataProvider);
  }

  @Component
  @ConditionalOnMissingBean
  public static ServiceMetadataProvider serviceMetadataProvider() {
    return new DefaultServiceMetadataProvider();
  }

  @Component
  @ConditionalOnMissingBean
  public static ServiceInterfaceMetadataProvider<ServiceMethod> serviceInterfaceMetadataProvider(ServiceMetadataProvider serviceMetadataProvider) {
    return new DefaultServiceInterfaceMetadataProvider(serviceMetadataProvider);
  }

  @Component
  public static InetService inetService(InetProperties inetProperties) {
    return new InetService(inetProperties);
  }

  @Component
  public static RequestDeserializer requestDeserializer(List<ArgumentSerialization> argumentSerializations,
          ServiceInterfaceMetadataProvider<ServiceMethod> serviceInterfaceMetadataProvider,
          ResourceLoader resourceLoader, LocalServiceHolder localServiceHolder) {
    List<ArgumentSerialization> serializations = TodayStrategies.find(ArgumentSerialization.class, resourceLoader.getClassLoader());
    argumentSerializations.addAll(serializations); // order after ArgumentSerialization beans
    return new RequestDeserializer(argumentSerializations, serviceInterfaceMetadataProvider, localServiceHolder);
  }

  @Component
  public static ResponseSerializer responseSerializer(List<ReturnValueSerializer> returnValueSerializers, ResourceLoader resourceLoader) {
    List<ReturnValueSerializer> serializations = TodayStrategies.find(ReturnValueSerializer.class, resourceLoader.getClassLoader());
    returnValueSerializers.addAll(serializations); // order after ReturnValueSerializer beans
    return new ResponseSerializer(returnValueSerializers);
  }

  @Component
  public static ServiceChannelHandler serviceChannelHandler(LocalServiceHolder localServiceHolder,
          RequestDeserializer requestDeserializer, ResponseSerializer responseSerializer) {
    return new ServiceChannelHandler(localServiceHolder, requestDeserializer, responseSerializer);
  }

  @Component
  @ConditionalOnMissingBean
  public static ResumeTokenGenerator resumeTokenGenerator() {
    return new RandomUUIDResumeTokenGenerator();
  }

  @Component
  @ConditionalOnMissingBean
  @ConditionalOnBooleanProperty(name = "today.service.server.resume.enabled", matchIfMissing = true)
  public static Resume remotingResume(@Nullable ResumableFramesStoreFactory storeFactory,
          ResumeTokenGenerator resumeTokenGenerator, ServiceServerProperties properties) {
    ResumeProperties resume = properties.resume;
    if (storeFactory == null) {
      storeFactory = new InMemoryResumableFramesStoreFactory("server", resume.getMemoryCacheLimit().toBytesInt());
    }
    return new Resume()
            .storeFactory(storeFactory)
            .token(resumeTokenGenerator)
            .streamTimeout(resume.getStreamTimeout())
            .sessionDuration(resume.getSessionDuration())
            .cleanupStoreOnKeepAlive(resume.isCleanupStoreOnKeepAlive());
  }

  @Component
  @ConditionalOnMissingBean
  public static ServerTransportFactory<? extends Closeable> serverTransportFactory(ServiceServerProperties properties) {
    return new TcpServerTransportFactory(properties);
  }

  @Component
  public static ServiceProviderServer serviceProviderServer(@Nullable Resume resume, ServiceServerProperties properties,
          ServiceChannelHandler serviceChannelHandler, ServerTransportFactory<? extends Closeable> serverTransportFactory) {
    return new ServiceProviderServer(properties, resume, serviceChannelHandler, serverTransportFactory);
  }

}
