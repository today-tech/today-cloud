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
