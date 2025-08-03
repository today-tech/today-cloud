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

/**
 * Contains key contracts of the RSocket programming model including {@link io.rsocket.Channel
 * RSocket} for performing or handling RSocket interactions, {@link io.rsocket.ChannelAcceptor
 * SocketAcceptor} for declaring responders, {@link io.rsocket.Payload Payload} for access to the
 * content of a payload, and others.
 *
 * <p>To connect to or start a server see {@link io.rsocket.core.ChannelConnector RSocketConnector}
 * and {@link io.rsocket.core.RemotingServer RemotingServer} in {@link io.rsocket.core}.
 */
@NonNullApi
package io.rsocket;

import infra.lang.NonNullApi;
