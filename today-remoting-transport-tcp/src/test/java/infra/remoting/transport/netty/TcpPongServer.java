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

package infra.remoting.transport.netty;

import infra.remoting.core.RemotingServer;
import infra.remoting.core.Resume;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.test.PingHandler;
import infra.remoting.transport.netty.server.TcpServerTransport;

public final class TcpPongServer {
  private static final boolean isResume =
          Boolean.valueOf(System.getProperty("REMOTING_TEST_RESUME", "false"));
  private static final int port = Integer.valueOf(System.getProperty("REMOTING_TEST_PORT", "7878"));

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
