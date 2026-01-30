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

package infra.cloud.serialize.format.value;

import java.time.Instant;

/**
 * Value representation of MessagePack's Timestamp type.
 */
public interface TimestampValue
        extends ExtensionValue {
  long getEpochSecond();

  int getNano();

  long toEpochMillis();

  Instant toInstant();
}
