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

import org.junit.jupiter.api.BeforeEach;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.time.Duration;

import javax.net.ssl.SSLException;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.test.TransportPair;
import io.rsocket.test.TransportTest;
import infra.remoting.transport.netty.client.WebsocketClientTransport;
import infra.remoting.transport.netty.server.WebsocketServerTransport;
import reactor.core.Exceptions;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;

final class WebsocketSecureTransportTests implements TransportTest {
  private TransportPair transportPair;

  @BeforeEach
  void createTestPair() {
    transportPair = new TransportPair<>(() -> new InetSocketAddress("localhost", 0), (address, server, allocator) ->
            WebsocketClientTransport.create(
                    HttpClient.create()
                            .option(ChannelOption.ALLOCATOR, allocator)
                            .remoteAddress(server::address)
                            .secure(ssl -> {
                              try {
                                ssl.sslContext(
                                        SslContextBuilder.forClient()
                                                .trustManager(InsecureTrustManagerFactory.INSTANCE).build());
                              }
                              catch (SSLException e) {
                                throw new RuntimeException(e);
                              }
                            }),
                    String.format(
                            "https://%s:%d/",
                            server.address().getHostName(), server.address().getPort())),
            (address, allocator) -> {
              try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                HttpServer server =
                        HttpServer.create()
                                .option(ChannelOption.ALLOCATOR, allocator)
                                .bindAddress(() -> address)
                                .secure(ssl -> {
                                  try {
                                    ssl.sslContext(
                                            SslContextBuilder.forServer(
                                                    ssc.certificate(), ssc.privateKey()).build());
                                  }
                                  catch (SSLException e) {
                                    throw new RuntimeException(e);
                                  }
                                });
                return WebsocketServerTransport.create(server);
              }
              catch (CertificateException e) {
                throw Exceptions.propagate(e);
              }
            });
  }

  @Override
  public Duration getTimeout() {
    return Duration.ofMinutes(5);
  }

  @Override
  public TransportPair getTransportPair() {
    return transportPair;
  }
}
