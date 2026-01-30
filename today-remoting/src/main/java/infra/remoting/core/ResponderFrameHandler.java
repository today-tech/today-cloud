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
package infra.remoting.core;

import infra.logging.Logger;
import infra.logging.LoggerFactory;

interface ResponderFrameHandler extends FrameHandler {

  Logger logger = LoggerFactory.getLogger(ResponderFrameHandler.class);

  @Override
  default void handleComplete() { }

  @Override
  default void handleError(Throwable t) {
    logger.debug("Dropped error", t);
    handleCancel();
  }

  @Override
  default void handleRequestN(long n) {
    // no ops
  }
}
