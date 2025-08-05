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

/**
 * Default {@link ChannelRequesterObservationConvention} implementation.
 *
 * @author Marcin Grzejszczak
 */
class DefaultChannelObservationConvention {

  private final ChannelContext channelContext;

  public DefaultChannelObservationConvention(ChannelContext channelContext) {
    this.channelContext = channelContext;
  }

  String getName() {
    if (this.channelContext.frameType == FrameType.REQUEST_FNF) {
      return "infra.remoting.fnf";
    }
    else if (this.channelContext.frameType == FrameType.REQUEST_STREAM) {
      return "infra.remoting.stream";
    }
    else if (this.channelContext.frameType == FrameType.REQUEST_CHANNEL) {
      return "infra.remoting.channel";
    }
    return "%s";
  }

  protected ChannelContext getChannelContext() {
    return this.channelContext;
  }
}
