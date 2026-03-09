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

import java.util.Objects;

import infra.core.style.ToStringBuilder;

/**
 * Metadata representing a service, including its unique identifier and version.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 21:51
 */
public class ServiceMetadata {

  private final String id;

  private final String version;

  public ServiceMetadata(String id, String version) {
    this.id = id;
    this.version = version;
  }

  public String getId() {
    return id;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ServiceMetadata that))
      return false;
    return Objects.equals(id, that.id)
            && Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("id", id)
            .append("version", version)
            .toString();
  }

}
