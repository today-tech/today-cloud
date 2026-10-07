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

package infra.remoting.plugins;

import infra.remoting.Channel;

/**
 * Contract to decorate an {@link Channel}, providing a way to intercept interactions. This can be
 * applied to a {@link InterceptorRegistry#forRequester(ChannelDecorator) requester} or {@link
 * InterceptorRegistry#forResponder(ChannelDecorator) responder} {@code Channel} of a client or
 * server.
 */
@FunctionalInterface
public interface ChannelDecorator {

  /**
   * Decorate Channel
   */
  Channel decorate(Channel channel);

}
