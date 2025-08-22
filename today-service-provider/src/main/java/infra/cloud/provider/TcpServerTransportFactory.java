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
