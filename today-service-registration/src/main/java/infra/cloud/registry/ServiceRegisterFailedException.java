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

package infra.cloud.registry;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

import infra.cloud.RemotingException;
import infra.cloud.client.Registration;

/**
 * @author TODAY 2021/7/11 17:19
 */
public class ServiceRegisterFailedException extends RemotingException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Registration registration;

  public ServiceRegisterFailedException(Registration registration, @Nullable Throwable cause) {
    super("Service register failed", cause);
    this.registration = registration;
  }

  public Registration getRegistration() {
    return registration;
  }

}

