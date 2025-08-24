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

package infra.cloud.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.cloud.client.Registration;
import infra.cloud.provider.ServicesProvider;
import infra.cloud.registry.event.InstancePreRegisteredEvent;
import infra.cloud.registry.event.InstanceRegisteredEvent;
import infra.cloud.service.ServiceMetadata;
import infra.context.SmartLifecycle;
import infra.context.support.ApplicationObjectSupport;

/**
 * Lifecycle methods that may be useful and common to {@link ServiceRegistry}
 * implementations.
 *
 * @param <R> Registration type passed to the {@link ServiceRegistry}.
 * @author Spencer Gibb
 * @author Zen Huifer
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public abstract class AbstractAutoServiceRegistration<R extends Registration, S>
        extends ApplicationObjectSupport implements AutoServiceRegistration, SmartLifecycle {

  protected final ServiceRegistry<R, S> serviceRegistry;

  private final AtomicBoolean running = new AtomicBoolean(false);

  private final List<RegistrationLifecycle<R>> registrationLifecycles;

  private final ArrayList<R> registrations = new ArrayList<>();

  private final ServicesProvider servicesProvider;

  protected AbstractAutoServiceRegistration(ServiceRegistry<R, S> serviceRegistry,
          List<RegistrationLifecycle<R>> registrationLifecycles, ServicesProvider servicesProvider) {
    this.serviceRegistry = serviceRegistry;
    this.registrationLifecycles = registrationLifecycles;
    this.servicesProvider = servicesProvider;
  }

  public void addRegistrationLifecycle(RegistrationLifecycle<R> registrationLifecycle) {
    this.registrationLifecycles.add(registrationLifecycle);
  }

  @Override
  public void start() {
    if (!isEnabled()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Discovery Lifecycle disabled. Not starting");
      }
      return;
    }

    logger.info("Registering services to registry: [{}]", serviceRegistry);

    if (!running.get()) {
      RegistrationFactory<R> registrationFactory = getRegistrationFactory();
      for (ServiceMetadata serviceMetadata : servicesProvider.getServices()) {
        R registration = registrationFactory.createRegistration(serviceMetadata);
        obtainApplicationContext().publishEvent(new InstancePreRegisteredEvent(this, registration));

        for (RegistrationLifecycle<R> lifecycle : registrationLifecycles) {
          lifecycle.postProcessBeforeStartRegister(registration);
        }
        register(registration);
        for (RegistrationLifecycle<R> lifecycle : registrationLifecycles) {
          lifecycle.postProcessAfterStartRegister(registration);
        }

        obtainApplicationContext().publishEvent(new InstanceRegisteredEvent<>(this, registration, getConfiguration()));
        registrations.add(registration);
      }
      running.compareAndSet(false, true);
    }
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public int getPhase() {
    return 0;
  }

  /**
   * Register the local service with the {@link ServiceRegistry}.
   */
  protected void register(R registration) {
    logger.debug("Registering registration: [{}]", registration);
    serviceRegistry.register(registration);
  }

  /**
   * un-register the local service with the {@link ServiceRegistry}.
   */
  protected void unregister(R registration) {
    logger.debug("Unregistering registration: [{}]", registration);
    serviceRegistry.unregister(registration);
  }

  /**
   * Go offline to delete the service registered on the machine
   */
  @Override
  public void stop() {
    if (running.compareAndSet(true, false) && isEnabled()) {
      logger.info("Un-Registering services: [{}]", serviceRegistry);
      for (R registration : registrations) {
        for (RegistrationLifecycle<R> lifecycle : registrationLifecycles) {
          lifecycle.postProcessBeforeStopRegister(registration);
        }

        unregister(registration);

        for (RegistrationLifecycle<R> lifecycle : registrationLifecycles) {
          lifecycle.postProcessAfterStopRegister(registration);
        }

        serviceRegistry.close();
      }
    }
  }

  /**
   * @return The object used to configure the registration.
   */
  protected abstract Object getConfiguration();

  /**
   * @return True, if this is enabled.
   */
  protected abstract boolean isEnabled();

  protected abstract RegistrationFactory<R> getRegistrationFactory();

}
