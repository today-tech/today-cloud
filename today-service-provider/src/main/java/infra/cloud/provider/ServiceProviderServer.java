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

import infra.context.Lifecycle;
import infra.lang.Nullable;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Closeable;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.core.RemotingServer;
import infra.remoting.core.Resume;
import infra.remoting.frame.decoder.PayloadDecoder;
import infra.remoting.transport.ServerTransport;
import infra.remoting.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/21 22:25
 */
public class ServiceProviderServer implements Lifecycle, ChannelAcceptor {

  private final ServiceChannelHandler channelHandler;

  private final Resume resume = new Resume();

  private int maxFrameLength;

  private String bindAddress;

  private int port;

  @Nullable
  private Closeable serverCloseable;

  public ServiceProviderServer(ServiceChannelHandler channelHandler) {
    this.channelHandler = channelHandler;
  }

  @Override
  public void start() {
    serverCloseable = RemotingServer.create(this)
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .resume(resume)
            .bindNow(createServerTransport());
  }

  private ServerTransport<? extends Closeable> createServerTransport() {
    return TcpServerTransport.create(bindAddress, port);
  }

  @Override
  public void stop() {
    if (serverCloseable != null) {
      serverCloseable.dispose();
    }
  }

  @Override
  public boolean isRunning() {
    return serverCloseable != null;
  }

  @Override
  public Mono<Channel> accept(ConnectionSetupPayload setup, Channel channel) {
    return Mono.just(channelHandler);
  }

}
