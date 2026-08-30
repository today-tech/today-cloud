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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2026/3/10 16:17
 */
class ServiceMetadataTests {

  @Test
  void testService() {
    ServiceMetadata metadata = new ServiceMetadata("test-service", "1.0", List.of());

    ServiceMetadata withProps = metadata.withProperties(Map.of(
            "env", "test"
    ));

    assertThat(metadata.getProperties()).isEmpty();
    assertThat(withProps.getProperties()).hasSize(1);

  }

  @Test
  void createWithRequiredFields() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of("com.example.UserService"));

    assertThat(metadata.getId()).isEqualTo("user-service");
    assertThat(metadata.getVersion()).isEqualTo("1.0.0");
    assertThat(metadata.getInterfaces()).containsExactly("com.example.UserService");
    assertThat(metadata.getProperties()).isEmpty();
  }

  @Test
  void createWithNullVersion() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", null, List.of("com.example.UserService"));

    assertThat(metadata.getId()).isEqualTo("user-service");
    assertThat(metadata.getVersion()).isNull();
    assertThat(metadata.getInterfaces()).containsExactly("com.example.UserService");
  }

  @Test
  void createWithEmptyInterfaces() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata.getId()).isEqualTo("user-service");
    assertThat(metadata.getVersion()).isEqualTo("1.0.0");
    assertThat(metadata.getInterfaces()).isEmpty();
  }

  @Test
  void createWithProperties() {
    Map<String, String> props = Map.of("env", "test", "region", "us-west-2");
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of(), props);

    assertThat(metadata.getProperties()).hasSize(2)
            .containsEntry("env", "test")
            .containsEntry("region", "us-west-2");
  }

  @Test
  void createWithNullProperties() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of(), null);

    assertThat(metadata.getProperties()).isEmpty();
  }

  @Test
  void createWithMultipleInterfaces() {
    List<String> interfaces = List.of("com.example.UserService", "com.example.UserAdminService");
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", interfaces);

    assertThat(metadata.getInterfaces()).hasSize(2)
            .containsExactly("com.example.UserService", "com.example.UserAdminService");
  }

  @Test
  void withPropertiesReturnsNewInstance() {
    ServiceMetadata original = new ServiceMetadata("user-service", "1.0.0", List.of());
    Map<String, String> newProps = Map.of("env", "prod");

    ServiceMetadata modified = original.withProperties(newProps);

    assertThat(modified).isNotSameAs(original);
    assertThat(original.getProperties()).isEmpty();
    assertThat(modified.getProperties()).containsEntry("env", "prod");
  }

  @Test
  void withPropertiesWithNullMap() {
    ServiceMetadata original = new ServiceMetadata("user-service", "1.0.0", List.of(), Map.of("env", "test"));
    ServiceMetadata modified = original.withProperties(null);

    assertThat(modified.getProperties()).isEmpty();
  }

  @Test
  void withPropertiesPreservesOtherFields() {
    ServiceMetadata original = new ServiceMetadata("user-service", "2.0.0", List.of("com.example.UserService"));
    ServiceMetadata modified = original.withProperties(Map.of("new-key", "new-value"));

    assertThat(modified.getId()).isEqualTo("user-service");
    assertThat(modified.getVersion()).isEqualTo("2.0.0");
    assertThat(modified.getInterfaces()).containsExactly("com.example.UserService");
    assertThat(modified.getProperties()).containsEntry("new-key", "new-value");
  }

  @Test
  void getPropertyReturnsValue() {
    Map<String, String> props = Map.of("env", "production", "timeout", "30s");
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of(), props);

    assertThat(metadata.getProperty("env")).isEqualTo("production");
    assertThat(metadata.getProperty("timeout")).isEqualTo("30s");
  }

  @Test
  void getPropertyReturnsNullForMissingKey() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata.getProperty("nonexistent")).isNull();
  }

  @Test
  void hasPropertyReturnsTrueWhenExists() {
    Map<String, String> props = Map.of("env", "test");
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of(), props);

    assertThat(metadata.hasProperty("env")).isTrue();
  }

  @Test
  void hasPropertyReturnsFalseWhenNotExists() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata.hasProperty("env")).isFalse();
  }

  @Test
  void hasPropertyReturnsFalseForEmptyMetadata() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata.hasProperty("any-key")).isFalse();
  }

  @Test
  void equalsReturnsTrueForSameObject() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata).isEqualTo(metadata);
  }

  @Test
  void equalsReturnsFalseForNull() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata).isNotEqualTo(null);
  }

  @Test
  void equalsReturnsFalseForDifferentType() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata).isNotEqualTo("not a ServiceMetadata");
  }

  @Test
  void equalsReturnsTrueForSameValues() {
    ServiceMetadata metadata1 = new ServiceMetadata("user-service", "1.0.0", List.of("com.example.UserService"));
    ServiceMetadata metadata2 = new ServiceMetadata("user-service", "1.0.0", List.of("com.example.UserService"));

    assertThat(metadata1).isEqualTo(metadata2);
  }

  @Test
  void equalsReturnsFalseForDifferentId() {
    ServiceMetadata metadata1 = new ServiceMetadata("user-service", "1.0.0", List.of());
    ServiceMetadata metadata2 = new ServiceMetadata("order-service", "1.0.0", List.of());

    assertThat(metadata1).isNotEqualTo(metadata2);
  }

  @Test
  void equalsReturnsFalseForDifferentVersion() {
    ServiceMetadata metadata1 = new ServiceMetadata("user-service", "1.0.0", List.of());
    ServiceMetadata metadata2 = new ServiceMetadata("user-service", "2.0.0", List.of());

    assertThat(metadata1).isNotEqualTo(metadata2);
  }

  @Test
  void equalsReturnsFalseForDifferentProperties() {
    ServiceMetadata metadata1 = new ServiceMetadata("user-service", "1.0.0", List.of(), Map.of("env", "test"));
    ServiceMetadata metadata2 = new ServiceMetadata("user-service", "1.0.0", List.of(), Map.of("env", "prod"));

    assertThat(metadata1).isNotEqualTo(metadata2);
  }

  @Test
  void equalsReturnsTrueForNullVersionBoth() {
    ServiceMetadata metadata1 = new ServiceMetadata("user-service", null, List.of());
    ServiceMetadata metadata2 = new ServiceMetadata("user-service", null, List.of());

    assertThat(metadata1).isEqualTo(metadata2);
  }

  @Test
  void equalsReturnsTrueForEmptyPropertiesBoth() {
    ServiceMetadata metadata1 = new ServiceMetadata("user-service", "1.0.0", List.of());
    ServiceMetadata metadata2 = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata1).isEqualTo(metadata2);
  }

  @Test
  void hashCodeIsConsistent() {
    ServiceMetadata metadata1 = new ServiceMetadata("user-service", "1.0.0", List.of());
    ServiceMetadata metadata2 = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
  }

  @Test
  void hashCodeDiffersForDifferentId() {
    ServiceMetadata metadata1 = new ServiceMetadata("user-service", "1.0.0", List.of());
    ServiceMetadata metadata2 = new ServiceMetadata("order-service", "1.0.0", List.of());

    assertThat(metadata1.hashCode()).isNotEqualTo(metadata2.hashCode());
  }

  @Test
  void hashCodeDiffersForDifferentProperties() {
    ServiceMetadata metadata1 = new ServiceMetadata("user-service", "1.0.0", List.of(), Map.of("env", "test"));
    ServiceMetadata metadata2 = new ServiceMetadata("user-service", "1.0.0", List.of(), Map.of("env", "prod"));

    assertThat(metadata1.hashCode()).isNotEqualTo(metadata2.hashCode());
  }

  @Test
  void toStringContainsId() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata.toString()).contains("user-service");
  }

  @Test
  void toStringContainsVersion() {
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of());

    assertThat(metadata.toString()).contains("1.0.0");
  }

  @Test
  void unmodifiableInterfaces() {
    List<String> interfaces = new ArrayList<>(List.of("com.example.UserService"));
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", interfaces);

    interfaces.add("com.example.AnotherService");

    assertThat(metadata.getInterfaces()).hasSize(1)
            .containsExactly("com.example.UserService");
  }

  @Test
  void propertiesAreDefensivelyCopied() {
    Map<String, String> mutableProps = new HashMap<>();
    mutableProps.put("env", "test");
    ServiceMetadata metadata = new ServiceMetadata("user-service", "1.0.0", List.of(), mutableProps);

    mutableProps.put("new-key", "new-value");
    mutableProps.remove("env");

    assertThat(metadata.getProperties()).hasSize(1)
            .containsEntry("env", "test");
  }

}