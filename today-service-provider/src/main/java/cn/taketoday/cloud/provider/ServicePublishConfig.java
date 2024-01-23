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

package cn.taketoday.cloud.provider;

import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.cloud.registry.HttpRegistration;
import cn.taketoday.cloud.registry.ServiceRegistry;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.stereotype.Component;
import cn.taketoday.stereotype.Singleton;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/11/28 20:51
 */
@Configuration(proxyBeanMethods = false)
class ServicePublishConfig {

  private static final Logger log = LoggerFactory.getLogger(HttpServiceProviderConfig.class);

  @Singleton
  static LocalServiceHolder localServiceHolder() {
    return new LocalServiceHolder(9001);
  }

  @Component
  static ServiceProviderLifecycle serviceProviderLifecycle(ServiceRegistry<HttpRegistration> serviceRegistry, LocalServiceHolder serviceHolder) {
    return new ServiceProviderLifecycle(serviceRegistry, serviceHolder);
  }

  static class ServiceProviderLifecycle implements SmartLifecycle {
    final LocalServiceHolder serviceHolder;

    final ServiceRegistry<HttpRegistration> serviceRegistry;

    private final AtomicBoolean started = new AtomicBoolean();

    ServiceProviderLifecycle(ServiceRegistry<HttpRegistration> serviceRegistry, LocalServiceHolder serviceHolder) {
      this.serviceRegistry = serviceRegistry;
      this.serviceHolder = serviceHolder;
    }

    @Override
    public void start() {
      if (started.compareAndSet(false, true)) {
        log.info("Registering services to registry: [{}]", serviceRegistry);
        serviceRegistry.register(new HttpRegistration(serviceHolder.getServices())); // register to registry
      }
    }

    @Override
    public void stop() {
      throw new UnsupportedOperationException("Stop must not be invoked directly");
    }

    /**
     * Go offline to delete the service registered on the machine
     */
    @Override
    public void stop(Runnable callback) {
      if (started.compareAndSet(true, false)) {
        log.info("Un-Registering services: [{}]", serviceRegistry);
        try {
          HttpRegistration registration = new HttpRegistration(serviceHolder.getServices());
          serviceRegistry.unregister(registration);
        }
        finally {
          callback.run();
        }
      }
    }

    @Override
    public boolean isRunning() {
      return started.get();
    }

  }

}
