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

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * A Gradle task that generates service metadata properties file.
 * <p>
 * This task reads configuration from {@link ServiceMetadataExtension} and writes
 * a {@code service-metadata.properties} file to the build resources directory.
 * The generated file contains service identification, versioning, grouping,
 * description, and any additional custom properties.
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2026/3/10 21:59
 */
@DisableCachingByDefault(because = "Not worth caching")
public abstract class GenerateServiceMetadata extends DefaultTask {

  @TaskAction
  public void generate() throws IOException {
    Project project = getProject();
    ServiceMetadataExtension serviceMetadata = project.getExtensions().getByType(ServiceMetadataExtension.class);

    var outputDir = project.getLayout().getBuildDirectory().dir("resources/main/META-INF").get().getAsFile();
    File propertiesFile = new File(outputDir, "service-metadata.properties");
    createFileIfNecessary(propertiesFile);

    Properties properties = CollectionUtils.createSortedProperties(false);

    properties.setProperty("service.id", serviceMetadata.getServiceId().get());
    properties.setProperty("service.version", serviceMetadata.getServiceVersion().get());
    properties.setProperty("service.group", serviceMetadata.getServiceGroup().get());
    properties.setProperty("service.description", serviceMetadata.getServiceDescription().get());

    List<String> interfaces = ServiceClassFinder.findInterfaces(project.getRootDir());
    properties.setProperty("service.interfaces", StringUtils.collectionToCommaDelimitedString(interfaces));
    
    convertToStringValues(serviceMetadata.getAdditional().get())
            .forEach((name, value) -> {
              if (value != null) {
                if (!name.startsWith("service.")) {
                  name = "service." + name;
                }
                properties.put(name, value);
              }
            });

    try (FileOutputStream outputStream = new FileOutputStream(propertiesFile)) {
      properties.store(outputStream, "Generated service metadata - DO NOT EDIT");
    }

  }

  private void createFileIfNecessary(File file) throws IOException {
    if (file.exists()) {
      return;
    }
    File parent = file.getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IllegalStateException(
              "Cannot create parent directory for '" + file.getAbsolutePath() + "'");
    }
    if (!file.createNewFile()) {
      throw new IllegalStateException("Cannot create target file '" + file.getAbsolutePath() + "'");
    }
  }

  private Map<String, @Nullable String> convertToStringValues(Map<String, Object> input) {
    Map<String, @Nullable String> output = new LinkedHashMap<>();
    input.forEach((key, value) -> {
      if (value instanceof Provider<?> provider) {
        value = provider.getOrNull();
      }
      output.put(key, value != null ? value.toString() : null);
    });
    return output;
  }

}
