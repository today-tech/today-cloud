/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.cloud.registry;

import cn.taketoday.context.properties.ConfigurationProperties;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/9/4 16:28
 */
@ConfigurationProperties("registry")
public class RegistryProperties {

  /**
   * Registry address
   */
  private String url;

  /**
   * services
   */
  private final Services services = new Services();

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public Services getServices() {
    return services;
  }

  /**
   * Services
   */
  public static class Services {

    /**
     * Service Registry HTTP URI.
     */
    private String uri;

    public void setUri(String uri) {
      this.uri = uri;
    }

    public String getUri() {
      return uri;
    }
  }

}
