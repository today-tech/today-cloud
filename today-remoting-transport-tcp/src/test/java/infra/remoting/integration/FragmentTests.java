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

package infra.remoting.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import infra.remoting.Channel;
import infra.remoting.ChannelWrapper;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

public class FragmentTests {
  private Channel handler;
  private CloseableChannel server;
  private String message = null;
  private String metaData = null;
  private String responseMessage = null;

  private static Stream<Arguments> cases() {
    return Stream.of(Arguments.of(0, 64), Arguments.of(64, 0), Arguments.of(64, 64));
  }

  public void startup(int frameSize) {
    int randomPort = ThreadLocalRandom.current().nextInt(10_000, 20_000);
    StringBuilder message = new StringBuilder();
    StringBuilder responseMessage = new StringBuilder();
    StringBuilder metaData = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      message.append("REQUEST ");
      responseMessage.append("RESPONSE ");
      metaData.append("METADATA ");
    }
    this.message = message.toString();
    this.responseMessage = responseMessage.toString();
    this.metaData = metaData.toString();

    TcpServerTransport serverTransport = TcpServerTransport.create("localhost", randomPort);
    server =
            RemotingServer.create((setup, channel) -> Mono.just(new ChannelWrapper(handler)))
                    .fragment(frameSize)
                    .bind(serverTransport)
                    .block();
  }

  private Channel buildClient(int frameSize) {
    return ChannelConnector.create()
            .fragment(frameSize)
            .connect(TcpClientTransport.create(server.address()))
            .block();
  }

  @AfterEach
  public void cleanup() {
    server.dispose();
  }

  @ParameterizedTest
  @MethodSource("cases")
  void testFragmentNoMetaData(int clientFrameSize, int serverFrameSize) {
    startup(serverFrameSize);
    System.out.println(
            "-------------------------------------------------testFragmentNoMetaData-------------------------------------------------");
    handler =
            new Channel() {
              @Override
              public Flux<Payload> requestStream(Payload payload) {
                String request = payload.getDataUtf8();
                String metaData = payload.getMetadataUtf8();
                System.out.println("request message:   " + request);
                System.out.println("request metadata:  " + metaData);

                return Flux.just(DefaultPayload.create(responseMessage));
              }
            };

    Channel client = buildClient(clientFrameSize);

    System.out.println("original message:  " + message);
    System.out.println("original metadata: " + metaData);
    Payload payload = client.requestStream(DefaultPayload.create(message)).blockLast();
    System.out.println("response message:  " + payload.getDataUtf8());
    System.out.println("response metadata: " + payload.getMetadataUtf8());

    assertThat(responseMessage).isEqualTo(payload.getDataUtf8());
  }

  @ParameterizedTest
  @MethodSource("cases")
  void testFragmentRequestMetaDataOnly(int clientFrameSize, int serverFrameSize) {
    startup(serverFrameSize);
    System.out.println(
            "-------------------------------------------------testFragmentRequestMetaDataOnly-------------------------------------------------");
    handler =
            new Channel() {
              @Override
              public Flux<Payload> requestStream(Payload payload) {
                String request = payload.getDataUtf8();
                String metaData = payload.getMetadataUtf8();
                System.out.println("request message:   " + request);
                System.out.println("request metadata:  " + metaData);

                return Flux.just(DefaultPayload.create(responseMessage));
              }
            };

    Channel client = buildClient(clientFrameSize);

    System.out.println("original message:  " + message);
    System.out.println("original metadata: " + metaData);
    Payload payload = client.requestStream(DefaultPayload.create(message, metaData)).blockLast();
    System.out.println("response message:  " + payload.getDataUtf8());
    System.out.println("response metadata: " + payload.getMetadataUtf8());

    assertThat(responseMessage).isEqualTo(payload.getDataUtf8());
  }

  @ParameterizedTest
  @MethodSource("cases")
  void testFragmentBothMetaData(int clientFrameSize, int serverFrameSize) {
    startup(serverFrameSize);
    Payload responsePayload = DefaultPayload.create(responseMessage);
    System.out.println(
            "-------------------------------------------------testFragmentBothMetaData-------------------------------------------------");
    handler =
            new Channel() {
              @Override
              public Flux<Payload> requestStream(Payload payload) {
                String request = payload.getDataUtf8();
                String metaData = payload.getMetadataUtf8();
                System.out.println("request message:   " + request);
                System.out.println("request metadata:  " + metaData);

                return Flux.just(DefaultPayload.create(responseMessage, metaData));
              }

              @Override
              public Mono<Payload> requestResponse(Payload payload) {
                String request = payload.getDataUtf8();
                String metaData = payload.getMetadataUtf8();
                System.out.println("request message:   " + request);
                System.out.println("request metadata:  " + metaData);

                return Mono.just(DefaultPayload.create(responseMessage, metaData));
              }
            };

    Channel client = buildClient(clientFrameSize);

    System.out.println("original message:  " + message);
    System.out.println("original metadata: " + metaData);
    Payload payload = client.requestStream(DefaultPayload.create(message, metaData)).blockLast();
    System.out.println("response message:  " + payload.getDataUtf8());
    System.out.println("response metadata: " + payload.getMetadataUtf8());

    assertThat(responseMessage).isEqualTo(payload.getDataUtf8());
  }
}
