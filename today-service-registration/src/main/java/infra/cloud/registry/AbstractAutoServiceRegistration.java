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

package infra.cloud.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.cloud.client.Registration;
import infra.cloud.provider.ServicesProvider;
import infra.cloud.registry.event.InstancePreRegisteredEvent;
import infra.cloud.registry.event.InstanceRegisteredEvent;
import infra.cloud.service.ServiceMetadata;
import infra.context.ApplicationContext;
import infra.context.SmartLifecycle;
import infra.context.support.ApplicationObjectSupport;

/**
 * Provides common lifecycle management methods for {@link ServiceRegistry} implementations.
 * <p>
 * This abstract class handles the registration and un-registration processes of service
 * instances, including event publishing and lifecycle callbacks via {@link RegistrationLifecycle}.
 * It implements {@link SmartLifecycle} to integrate with the application context's lifecycle.
 * </p>
 *
 * @param <R> The type of {@link Registration} used by the {@link ServiceRegistry}.
 * @param <S> The type of configuration or source object associated with the registry.
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
      ApplicationContext context = applicationContext();
      RegistrationFactory<R> registrationFactory = getRegistrationFactory();
      for (ServiceMetadata serviceMetadata : servicesProvider.getServices()) {
        R registration = registrationFactory.createRegistration(serviceMetadata);
        context.publishEvent(new InstancePreRegisteredEvent(this, registration));

        for (RegistrationLifecycle<R> lifecycle : registrationLifecycles) {
          lifecycle.postProcessBeforeStartRegister(registration);
        }
        register(registration);
        for (RegistrationLifecycle<R> lifecycle : registrationLifecycles) {
          lifecycle.postProcessAfterStartRegister(registration);
        }

        context.publishEvent(new InstanceRegisteredEvent<>(this, registration, getConfiguration()));
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

  /**
   * Returns the factory used to create {@link Registration} instances for the services.
   *
   * @return the registration factory
   */
  protected abstract RegistrationFactory<R> getRegistrationFactory();

}
