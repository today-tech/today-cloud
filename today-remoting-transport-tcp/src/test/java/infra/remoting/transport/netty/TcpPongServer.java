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

import infra.remoting.core.RemotingServer;
import infra.remoting.core.Resume;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.test.PingHandler;
import infra.remoting.transport.netty.server.TcpServerTransport;

public final class TcpPongServer {
  private static final boolean isResume =
          Boolean.valueOf(System.getProperty("RSOCKET_TEST_RESUME", "false"));
  private static final int port = Integer.valueOf(System.getProperty("RSOCKET_TEST_PORT", "7878"));

  public static void main(String... args) {
    System.out.println("Starting TCP ping-pong server");
    System.out.println("port: " + port);
    System.out.println("resume enabled: " + isResume);

    RemotingServer server = RemotingServer.create(new PingHandler());
    if (isResume) {
      server.resume(new Resume());
    }
    server
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .bind(TcpServerTransport.create("localhost", port))
            .block()
            .onClose()
            .block();
  }
}
