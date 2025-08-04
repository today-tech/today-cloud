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

  Channel decorate(Channel channel);

}
