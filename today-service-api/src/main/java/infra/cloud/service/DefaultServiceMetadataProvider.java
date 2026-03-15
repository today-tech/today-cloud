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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;

import static infra.lang.TodayStrategies.readStrategies;

/**
 * Default implementation of {@link ServiceMetadataProvider} that loads service metadata
 * from properties files located on the classpath.
 * <p>
 * By default, this provider searches for configuration files at
 * {@code META-INF/service-metadata.properties}. It supports loading multiple
 * resources with the same name from different locations in the classpath and
 * aggregates the metadata into an internal cache keyed by interface name.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 21:55
 */
public class DefaultServiceMetadataProvider implements ServiceMetadataProvider {

  private static final Logger log = LoggerFactory.getLogger(DefaultServiceMetadataProvider.class);

  public static final String DEFAULT_METADATA_LOCATION = "META-INF/service-metadata.properties";

  public static final String KEY_PREFIX = "service.";

  public static final String KEY_SERVICE_ID = "service.id";
  public static final String KEY_SERVICE_VERSION = "service.version";
  public static final String KEY_SERVICE_INTERFACES = "service.interfaces";

  private final Map<String, ServiceMetadata> metadataCache;

  /**
   * Constructs a provider with the default metadata location.
   */
  public DefaultServiceMetadataProvider() {
    this(DEFAULT_METADATA_LOCATION);
  }

  /**
   * Constructs a provider with a custom metadata location.
   *
   * @param metadataLocation the classpath location of the metadata file
   */
  public DefaultServiceMetadataProvider(String metadataLocation) {
    Assert.notNull(metadataLocation, "metadata-location is required");
    this.metadataCache = loadMetadata(metadataLocation);
  }

  @Override
  public ServiceMetadata getMetadata(Class<?> serviceInterface) {
    ServiceMetadata metadata = metadataCache.get(serviceInterface.getName());
    Assert.state(metadata != null, "metadata not found");
    return metadata;
  }

  private Map<String, ServiceMetadata> loadMetadata(String metadataLocation) {
    Map<String, ServiceMetadata> metadataCache = new HashMap<>();
    List<ServiceMetadata> serviceMetadata = loadResources(ClassUtils.getDefaultClassLoader(), metadataLocation);
    for (ServiceMetadata metadata : serviceMetadata) {
      for (String ifc : metadata.getInterfaces()) {
        metadataCache.put(ifc, metadata);
      }
    }
    return metadataCache;
  }

  /**
   * Creates ServiceMetadata from loaded properties.
   *
   * @param props the loaded properties
   * @return a new ServiceMetadata instance
   */
  protected ServiceMetadata createMetadata(MultiValueMap<String, String> props) {
    String serviceId = CollectionUtils.firstElement(props.remove(KEY_SERVICE_ID));
    String version = CollectionUtils.firstElement(props.remove(KEY_SERVICE_VERSION));
    List<String> interfaces = props.remove(KEY_SERVICE_INTERFACES);

    Set<String> keys = props.keySet();
    var metadata = CollectionUtils.<String, String>newLinkedHashMap(keys.size());
    for (String key : keys) {
      String value = props.getFirst(key);
      if (StringUtils.hasText(value)) {
        metadata.put(StringUtils.delete(key, KEY_PREFIX), value);
      }
    }

    return new ServiceMetadata(serviceId, version, interfaces, metadata);
  }

  protected List<ServiceMetadata> loadResources(ClassLoader classLoader, String metadataLocation) {
    List<ServiceMetadata> serviceMetadata = new ArrayList<>();
    try {
      log.debug("Detecting service-metadata location '{}'", metadataLocation);
      Enumeration<URL> urls = classLoader.getResources(metadataLocation);
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        Properties properties = new Properties();

        log.debug("Reading service-metadata file '{}'", url);
        try (InputStream inputStream = url.openStream()) {
          properties.load(inputStream);
        }

        MultiValueMap<String, String> metadata = MultiValueMap.forLinkedHashMap();
        readStrategies(metadata, properties);
        serviceMetadata.add(createMetadata(metadata));
      }
    }
    catch (IOException ex) {
      throw new IllegalArgumentException(
              "Unable to load service-metadata from location [%s]".formatted(metadataLocation), ex);
    }
    return serviceMetadata;
  }

}
