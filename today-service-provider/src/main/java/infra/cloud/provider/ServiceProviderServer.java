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

import org.jspecify.annotations.Nullable;

import infra.context.SmartLifecycle;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Closeable;
import infra.remoting.ConnectionSetupPayload;
import infra.remoting.core.RemotingServer;
import infra.remoting.core.Resume;
import infra.remoting.frame.decoder.PayloadDecoder;
import reactor.core.publisher.Mono;

/**
 * The {@code ServiceProviderServer} is responsible for starting and managing the lifecycle of a service provider.
 * It implements {@link SmartLifecycle} to handle startup and shutdown processes, and {@link ChannelAcceptor}
 * to accept incoming connections and configure the communication channel with the appropriate handler.
 * <p>
 * This server utilizes a {@link ServiceChannelHandler} to process channels and relies on a
 * {@link ServerTransportFactory} to create the underlying transport mechanism. It also supports optional
 * session resumption via the {@link Resume} component.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/21 22:25
 */
public class ServiceProviderServer implements SmartLifecycle, ChannelAcceptor {

  private static final Logger log = LoggerFactory.getLogger(ServiceProviderServer.class);

  private final ServiceServerProperties properties;

  private final ServiceChannelHandler channelHandler;

  private final ServerTransportFactory<? extends Closeable> serverTransportFactory;

  @Nullable
  private final Resume resume;

  @Nullable
  private Closeable serverCloseable;

  public ServiceProviderServer(ServiceServerProperties properties, @Nullable Resume resume,
          ServiceChannelHandler channelHandler, ServerTransportFactory<? extends Closeable> serverTransportFactory) {
    this.properties = properties;
    this.resume = resume;
    this.channelHandler = channelHandler;
    this.serverTransportFactory = serverTransportFactory;
  }

  @Override
  public void start() {
    serverCloseable = RemotingServer.create(this)
            .resume(resume)
            .payloadDecoder(PayloadDecoder.ZERO_COPY)
            .fragment(properties.getMaxTransmissionUnit().toBytesInt())
            .maxTimeToFirstFrame(properties.getMaxTimeToFirstFrame())
            .maxInboundPayloadSize(properties.getMaxInboundPayloadSize().toBytesInt())
            .bindNow(serverTransportFactory.createTransport());

    log.info("Service provider server started on port: {}", properties.getPort());
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
  public boolean isPausable() {
    return false;
  }

  @Override
  public Mono<Channel> accept(ConnectionSetupPayload setup, Channel channel) {
    return Mono.just(channelHandler);
  }

}
