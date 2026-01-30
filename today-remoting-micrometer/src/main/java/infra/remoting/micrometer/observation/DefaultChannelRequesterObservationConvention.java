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

import infra.remoting.frame.FrameType;
import io.micrometer.common.KeyValues;
import io.micrometer.common.util.StringUtils;
import io.micrometer.observation.Observation;

/**
 * Default {@link ChannelRequesterObservationConvention} implementation.
 *
 * @author Marcin Grzejszczak
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public class DefaultChannelRequesterObservationConvention extends DefaultChannelObservationConvention implements ChannelRequesterObservationConvention {

  public DefaultChannelRequesterObservationConvention(ChannelContext channelContext) {
    super(channelContext);
  }

  @Override
  public KeyValues getLowCardinalityKeyValues(ChannelContext context) {
    KeyValues values = KeyValues.of(
            RemotingObservationDocumentation.ResponderTags.REQUEST_TYPE.withValue(context.frameType.name()));
    if (StringUtils.isNotBlank(context.route)) {
      values = values.and(RemotingObservationDocumentation.ResponderTags.ROUTE.withValue(context.route));
    }
    return values;
  }

  @Override
  public boolean supportsContext(Observation.Context context) {
    return context instanceof ChannelContext;
  }

  @Override
  public String getName() {
    if (getChannelContext().frameType == FrameType.REQUEST_RESPONSE) {
      return "infra.remoting.request";
    }
    return super.getName();
  }
}
