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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import infra.util.StringUtils;

/**
 * Reads {@link ServiceMetadata} from configuration files located in
 * {@code META-INF/service-metadata.properties} on the classpath.
 * <p>
 * This implementation supports multiple loading strategies:
 * </p>
 * <ol>
 *   <li>Load from {@code META-INF/service-metadata.properties} (development and production)</li>
 *   <li>Fallback to {@link Package} MANIFEST.MF if properties file is not found</li>
 *   <li>Ultimate fallback to package name if neither metadata source is available</li>
 * </ol>
 * <p>
 * Metadata instances are cached per package to ensure memory efficiency and safe sharing.
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 21:55
 */
public class PackageInfoServiceMetadataProvider implements ServiceMetadataProvider {

  public static final String DEFAULT_METADATA_LOCATION = "META-INF/service-metadata.properties";

  public static final String KEY_SERVICE_ID = "service.id";
  public static final String KEY_SERVICE_VERSION = "service.version";
  public static final String KEY_SERVICE_GROUP = "service.group";
  public static final String KEY_SERVICE_DESCRIPTION = "service.description";

  /**
   * Cache to store ServiceMetadata instances per package.
   */
  private final Map<String, ServiceMetadata> metadataCache = new ConcurrentHashMap<>();

  private final String metadataLocation;

  /**
   * Constructs a provider with the default metadata location.
   */
  public PackageInfoServiceMetadataProvider() {
    this(DEFAULT_METADATA_LOCATION);
  }

  /**
   * Constructs a provider with a custom metadata location.
   *
   * @param metadataLocation the classpath location of the metadata file
   */
  public PackageInfoServiceMetadataProvider(String metadataLocation) {
    this.metadataLocation = metadataLocation;
  }

  @Override
  public ServiceMetadata getMetadata(Class<?> serviceInterface) {
    return metadataCache.computeIfAbsent(getCacheKey(serviceInterface), key -> loadMetadata(serviceInterface));
  }

  /**
   * Loads metadata for the given service interface.
   * <p>
   * Loading strategy:
   * 1. Try to load from properties file
   * 2. Fallback to Package MANIFEST.MF
   * 3. Ultimate fallback to package name
   * </p>
   *
   * @param serviceInterface the service interface class
   * @return a ServiceMetadata instance (never null)
   */
  protected ServiceMetadata loadMetadata(Class<?> serviceInterface) {
    Properties props = loadProperties(serviceInterface);
    if (props != null) {
      return createMetadataFromProperties(props);
    }

    Package servicePackage = serviceInterface.getPackage();
    String specificationTitle = servicePackage.getSpecificationTitle();
    String specificationVersion = servicePackage.getSpecificationVersion();

    if (specificationTitle != null && !specificationTitle.isBlank()) {
      return new ServiceMetadata(specificationTitle, specificationVersion);
    }

    String packageName = serviceInterface.getPackageName();
    return new ServiceMetadata(packageName, null);
  }

  /**
   * Loads properties from the metadata file.
   *
   * @param serviceInterface the service interface class
   * @return loaded properties, or empty map if not found
   */
  protected @Nullable Properties loadProperties(Class<?> serviceInterface) {
    try {
      ClassLoader classLoader = serviceInterface.getClassLoader();
      if (classLoader == null) {
        classLoader = ClassLoader.getSystemClassLoader();
      }

      Enumeration<URL> urls = classLoader.getResources(metadataLocation);
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        try (InputStream input = url.openStream()) {
          Properties props = new Properties();
          props.load(input);

          // 验证是否包含必需的 service.id
          String serviceId = props.getProperty(KEY_SERVICE_ID);
          if (StringUtils.hasText(serviceId)) {
            return props;
          }
        }
      }
    }
    catch (IOException ex) {
      // 静默失败，降级到其他策略
    }

    return null;
  }

  /**
   * Creates ServiceMetadata from loaded properties.
   *
   * @param props the loaded properties
   * @return a new ServiceMetadata instance
   */
  protected ServiceMetadata createMetadataFromProperties(Properties props) {
    String serviceId = props.getProperty(KEY_SERVICE_ID);
    String version = props.getProperty(KEY_SERVICE_VERSION);

    Map<String, String> metadata = new LinkedHashMap<>();

    addPropertyIfPresent(metadata, props, KEY_SERVICE_DESCRIPTION);
    addPropertyIfPresent(metadata, props, KEY_SERVICE_GROUP);

    return new ServiceMetadata(serviceId, version, metadata);
  }

  /**
   * Helper method to add a property if it's present.
   */
  private void addPropertyIfPresent(Map<String, String> metadata, Properties props, String key) {
    String value = props.getProperty(key);
    if (value != null && !value.isBlank()) {
      metadata.put(key.substring(key.indexOf('.') + 1), value);
    }
  }

  /**
   * Generates a cache key for the given service interface.
   * <p>
   * Using the package name as the key ensures that all service interfaces
   * within the same package share the same ServiceMetadata.
   * </p>
   *
   * @param serviceInterface the service interface class
   * @return the cache key (package name)
   */
  protected String getCacheKey(Class<?> serviceInterface) {
    return serviceInterface.getPackageName();
  }

  /**
   * Clears the internal cache.
   */
  public void clearCache() {
    metadataCache.clear();
  }

  /**
   * Returns the number of cached metadata entries.
   *
   * @return the number of cached service metadata instances
   */
  public int getCacheSize() {
    return metadataCache.size();
  }

}
