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
