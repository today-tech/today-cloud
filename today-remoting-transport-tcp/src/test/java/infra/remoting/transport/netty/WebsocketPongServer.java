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

package infra.remoting.transport.netty;

import io.rsocket.core.RemotingServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.test.PingHandler;
import infra.remoting.transport.netty.server.WebsocketServerTransport;

public final class WebsocketPongServer {

  public static void main(String... args) {
    RemotingServer.create(new PingHandler())
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .bind(WebsocketServerTransport.create(7878))
            .block()
            .onClose()
            .block();
  }
}
