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

package infra.cloud.plugin;

import org.gradle.api.Project;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.io.Serializable;

/**
 * Extension for configuring service metadata in a Gradle project.
 * <p>
 * This class holds metadata such as the service ID, version, and additional custom properties.
 * It is designed to be used within a Gradle plugin to manage service-related configuration.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2026/3/10 21:41
 */
public abstract class ServiceMetadataExtension implements Serializable {

  /**
   * Creates a new {@code ServiceMetadataExtension} associated with the given {@code project}.
   * <p>
   * The service ID is defaulted to the project name, and the service version is defaulted
   * to the project's version string.
   *
   * @param project the Gradle project to associate with this extension
   */
  public ServiceMetadataExtension(Project project) {
    getServiceId().convention(project.provider(project::getName));
    getServiceDescription().convention(project.provider(project::getDescription));
    getServiceVersion().convention(project.provider(() -> project.getVersion().toString()));
    getServiceGroup().convention("default");
  }

  /**
   * Gets the property representing the unique identifier of the service.
   * <p>
   * This property is marked as an input for Gradle's up-to-date checks.
   *
   * @return the service ID property
   */
  @Input
  @Optional
  public abstract Property<String> getServiceId();

  /**
   * Gets the property representing the version of the service.
   * <p>
   * This property is optional and marked as an input for Gradle's up-to-date checks.
   *
   * @return the service version property, or null if not set
   */
  @Input
  @Optional
  public abstract Property<String> getServiceVersion();

  @Input
  @Optional
  public abstract Property<String> getServiceDescription();

  @Input
  @Optional
  public abstract Property<String> getServiceGroup();

  /**
   * Gets the map property for storing additional custom metadata.
   * <p>
   * This property is internal and not considered for Gradle's up-to-date checks.
   *
   * @return the map of additional metadata
   */
  @Optional
  public abstract MapProperty<String, Object> getAdditional();

}
