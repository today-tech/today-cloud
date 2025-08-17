/*
 * Copyright 2021 - 2024 the original author or authors.
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

package infra.cloud.service;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/9 15:45
 */
public enum InvocationType {

  /**
   * One request message without response.
   */
  FIRE_AND_FORGET,

  /**
   * One request message followed by one response message.
   */
  REQUEST_RESPONSE,

  /**
   * One request message followed by zero or more response messages.
   */
  RESPONSE_STREAMING,

  /**
   * Zero or more request and response messages arbitrarily interleaved in time.
   */
  DUPLEX_STREAMING;

  /**
   * Returns {@code true} for {@code REQUEST_RESPONSE} and {@code RESPONSE_STREAMING}, which do not permit the
   * client to stream.
   */
  public final boolean clientSendsOneMessage() {
    return this == REQUEST_RESPONSE || this == RESPONSE_STREAMING;
  }

  /**
   * Returns {@code true} for {@code REQUEST_RESPONSE}, which do not permit the
   * server to stream.
   */
  public final boolean serverSendsOneMessage() {
    return this == REQUEST_RESPONSE;
  }

}
