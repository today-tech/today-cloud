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

package infra.remoting.micrometer.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * {@link ObservationConvention} for responder {@link ChannelContext}.
 *
 * @author Marcin Grzejszczak
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public interface ChannelResponderObservationConvention extends ObservationConvention<ChannelContext> {

  @Override
  default boolean supportsContext(Observation.Context context) {
    return context instanceof ChannelContext
            && ((ChannelContext) context).side == ChannelContext.Side.RESPONDER;
  }
}
