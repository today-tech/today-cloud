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

package infra.cloud.provider.annotation.config;

import infra.cloud.client.annotation.ConditionalOnDiscoveryEnabled;
import infra.cloud.net.InetProperties;
import infra.cloud.net.InetService;
import infra.cloud.provider.DefaultServiceInterfaceMetadataProvider;
import infra.cloud.provider.LocalServiceHolder;
import infra.cloud.provider.ServerTransportFactory;
import infra.cloud.provider.ServiceChannelHandler;
import infra.cloud.provider.ServiceProviderServer;
import infra.cloud.provider.ServiceServerProperties;
import infra.cloud.provider.TcpServerTransportFactory;
import infra.cloud.registry.annotation.config.AutoServiceRegistrationAutoConfiguration;
import infra.cloud.service.PackageInfoServiceMetadataProvider;
import infra.cloud.service.ServiceInterfaceMetadataProvider;
import infra.cloud.service.ServiceMetadataProvider;
import infra.cloud.service.ServiceMethod;
import infra.cloud.service.config.ResumeProperties;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnBooleanProperty;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.lang.Nullable;
import infra.remoting.Closeable;
import infra.remoting.core.Resume;
import infra.remoting.resume.InMemoryResumableFramesStoreFactory;
import infra.remoting.resume.RandomUUIDResumeTokenGenerator;
import infra.remoting.resume.ResumableFramesStoreFactory;
import infra.remoting.resume.ResumeTokenGenerator;
import infra.stereotype.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/10 22:31
 */
@ConditionalOnDiscoveryEnabled
@EnableConfigurationProperties({ InetProperties.class, ServiceServerProperties.class })
@DisableDIAutoConfiguration(before = AutoServiceRegistrationAutoConfiguration.class)
public class ServiceProviderAutoConfiguration {

  @Component
  public static LocalServiceHolder localServiceHolder() {
    return new LocalServiceHolder();
  }

  @Component
  @ConditionalOnMissingBean
  public static ServiceMetadataProvider serviceMetadataProvider() {
    return new PackageInfoServiceMetadataProvider();
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
  public static ServiceChannelHandler serviceChannelHandler() {
    return new ServiceChannelHandler();
  }

  @Component
  @ConditionalOnMissingBean
  public static ResumeTokenGenerator resumeTokenGenerator() {
    return new RandomUUIDResumeTokenGenerator();
  }

  @Component
  @ConditionalOnMissingBean
  @ConditionalOnBooleanProperty(name = "service.server.resume.enabled", matchIfMissing = true)
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
