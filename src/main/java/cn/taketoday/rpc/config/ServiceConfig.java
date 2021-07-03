/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.rpc.config;

/**
 * service config
 *
 * @param <T>
 *         Service Type
 *
 * @author TODAY 2021/7/3 22:40
 */
public class ServiceConfig<T> {

  /** service interface */
  private Class<T> serviceInterface;

  /** reference instance */
  private T reference;

  /**
   * Set service instance
   */
  public void setReference(T reference) {
    this.reference = reference;
  }

  /**
   * Set service interface
   *
   * @param serviceInterface
   *         service interface
   */
  public void setInterface(Class<T> serviceInterface) {
    this.serviceInterface = serviceInterface;
  }

  /**
   * Get service interface
   */
  public Class<T> getInterface() {
    return serviceInterface;
  }

  /**
   * Get service instance
   */
  public T getReference() {
    return reference;
  }

}
