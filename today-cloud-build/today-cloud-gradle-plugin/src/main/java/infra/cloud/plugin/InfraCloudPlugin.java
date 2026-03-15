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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;

import infra.gradle.plugin.InfraApplicationPlugin;
import infra.lang.VersionExtractor;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2026/3/10 21:31
 */
public class InfraCloudPlugin implements Plugin<Project> {

  private static final String INFRA_CLOUD_VERSION = VersionExtractor.forClass(InfraCloudPlugin.class);

  public static final String GENERATE_SERVICE_METADATA_TASK_NAME = "generateServiceMetadata";

  /**
   * The coordinates {@code (group:name:version)} of the
   * {@code infra-cloud-dependencies} bom.
   */
  public static final String BOM_COORDINATES = "cn.taketoday:today-cloud-dependencies:" + INFRA_CLOUD_VERSION;

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaPlugin.class);
    project.getPlugins().apply(JavaLibraryPlugin.class);
    project.getPlugins().apply(InfraApplicationPlugin.class);

    TaskContainer tasks = project.getTasks();
    createExtensions(project);

    tasks.register(GENERATE_SERVICE_METADATA_TASK_NAME, GenerateServiceMetadata.class, task -> {
      task.setGroup(BasePlugin.BUILD_GROUP);
      task.setDescription("Generates a service-metadata.properties file.");
    });

    tasks.getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)
            .dependsOn(GENERATE_SERVICE_METADATA_TASK_NAME);

    project.getExtensions().getByType(DependencyManagementExtension.class)
            .imports(imports -> imports.mavenBom(BOM_COORDINATES));
  }

  private void createExtensions(Project project) {
    project.getExtensions().create("serviceMetadata", ServiceMetadataExtension.class, project);
    project.getExtensions().create("cloudApplication", CloudApplicationExtension.class, project);
  }

}
