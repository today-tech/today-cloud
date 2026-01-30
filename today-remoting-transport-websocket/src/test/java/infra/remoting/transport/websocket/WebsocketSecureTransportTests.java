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

package infra.remoting.transport.websocket;

import org.junit.jupiter.api.BeforeEach;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.time.Duration;

import javax.net.ssl.SSLException;

import infra.remoting.test.TransportPair;
import infra.remoting.test.TransportTest;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
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
