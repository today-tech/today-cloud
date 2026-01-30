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

package infra.cloud.provider;

import infra.lang.Assert;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.util.DataSize;
import reactor.netty.tcp.TcpServer;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/22 22:20
 */
public class TcpServerTransportFactory implements ServerTransportFactory<CloseableChannel> {

  private final ServiceServerProperties properties;

  public TcpServerTransportFactory(ServiceServerProperties properties) {
    Assert.notNull(properties, "properties is required");
    this.properties = properties;
  }

  @Override
  public TcpServerTransport createTransport() {
    String bindAddress = properties.getBindAddress();
    DataSize maxFrameLength = properties.getMaxFrameLength();
    if (bindAddress != null) {
      TcpServer server = TcpServer.create().host(bindAddress).port(properties.getPort());
      return TcpServerTransport.create(server, maxFrameLength.toBytesInt());
    }
    return TcpServerTransport.create(TcpServer.create().port(properties.getPort()), maxFrameLength.toBytesInt());
  }

}
