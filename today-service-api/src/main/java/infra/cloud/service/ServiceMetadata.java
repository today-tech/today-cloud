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

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import infra.core.style.ToStringBuilder;
import infra.lang.Assert;

/**
 * Represents metadata for a service artifact (e.g., a JAR file) that may contain
 * one or more service interfaces.
 * <p>
 * This class encapsulates the service's identity information including its unique identifier,
 * version, and service-level properties.
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 21:51
 */
public class ServiceMetadata {

  private final String id;

  private final @Nullable String version;

  private final List<String> interfaces;

  private final Map<String, String> properties;

  /**
   * Constructs a new {@code ServiceMetadata} with the specified id and version.
   *
   * @param id the unique identifier of the service
   * @param version the version of the service, may be {@code null}
   */
  public ServiceMetadata(String id, @Nullable String version, List<String> interfaces) {
    this(id, version, interfaces, null);
  }

  /**
   * Constructs a new {@code ServiceMetadata} with the specified id, version, and properties.
   *
   * @param id the unique identifier of the service
   * @param version the version of the service, may be {@code null}
   * @param properties the properties
   * @param interfaces interfaces class names
   */
  public ServiceMetadata(String id, @Nullable String version, List<String> interfaces, @Nullable Map<String, String> properties) {
    Assert.notNull(id, "service id is required");
    Assert.notNull(interfaces, "interfaces is required");
    this.id = id;
    this.version = version;
    this.interfaces = interfaces;
    this.properties = properties == null ? Map.of() : Map.copyOf(properties);
  }

  /**
   * Returns the unique identifier of the service.
   *
   * @return the service ID
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the version of the service, which may be {@code null} if not specified.
   *
   * @return the service version, or {@code null}
   */
  public @Nullable String getVersion() {
    return version;
  }

  /**
   * Returns the list of service interface class names contained in this artifact.
   * <p>
   * The returned list is unmodifiable and represents a snapshot of the interfaces
   * at construction time. This ensures thread-safety and prevents accidental modification.
   * </p>
   *
   * @return the list of interface class names (never null)
   */
  public List<String> getInterfaces() {
    return interfaces;
  }

  /**
   * Returns the properties associated with this service artifact.
   * <p>
   * The returned map is unmodifiable and represents a snapshot of the properties
   * at construction time. This ensures thread-safety and prevents accidental modification.
   * </p>
   *
   * @return the properties map (never null)
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Creates a new {@code ServiceMetadata} instance with the specified properties,
   * while retaining the current {@code id} and {@code version}.
   * <p>
   * This method follows an immutable pattern, returning a new instance rather than
   * modifying the existing one. If the provided properties map is {@code null},
   * the resulting instance will have an empty properties map.
   * </p>
   *
   * @param properties the new properties to associate with the service, may be {@code null}
   * @return a new {@code ServiceMetadata} instance with the updated properties
   */
  public ServiceMetadata withProperties(@Nullable Map<String, String> properties) {
    return new ServiceMetadata(id, version, interfaces, properties);
  }

  /**
   * Convenience method to retrieve a specific property value.
   *
   * @param key the property key
   * @return the property value, or {@code null} if not present
   */
  public @Nullable String getProperty(String key) {
    return this.properties.get(key);
  }

  /**
   * Checks if a property exists for the given key.
   *
   * @param key the property key
   * @return {@code true} if the property exists, {@code false} otherwise
   */
  public boolean hasProperty(String key) {
    return this.properties.containsKey(key);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ServiceMetadata that))
      return false;
    return Objects.equals(id, that.id)
            && Objects.equals(version, that.version)
            && Objects.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version, properties);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("id", id)
            .append("version", version)
            .append("interfaces", interfaces)
            .append("properties", properties)
            .toString();
  }

}
