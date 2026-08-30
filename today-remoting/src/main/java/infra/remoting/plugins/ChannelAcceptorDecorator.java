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

import infra.remoting.ChannelAcceptor;

/**
 * Contract to decorate a {@link ChannelAcceptor}, providing access to connection {@code setup}
 * information and the ability to also decorate the channels for requesting and responding.
 *
 * <p>This could be used as an alternative to registering an individual "requester" {@code
 * ChannelInterceptor} and "responder" {@code ChannelInterceptor}.
 */
@FunctionalInterface
public interface ChannelAcceptorDecorator {

  ChannelAcceptor decorate(ChannelAcceptor delegate);

}
