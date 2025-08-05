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
