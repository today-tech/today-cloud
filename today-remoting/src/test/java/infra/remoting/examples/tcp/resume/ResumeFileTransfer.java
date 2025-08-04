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

package infra.remoting.examples.tcp.resume;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import infra.remoting.Channel;
import infra.remoting.ChannelAcceptor;
import infra.remoting.Payload;
import infra.remoting.core.ChannelConnector;
import infra.remoting.core.RemotingServer;
import infra.remoting.core.Resume;
import infra.remoting.transport.netty.client.TcpClientTransport;
import infra.remoting.transport.netty.server.CloseableChannel;
import infra.remoting.transport.netty.server.TcpServerTransport;
import infra.remoting.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

public class ResumeFileTransfer {

  /*amount of file chunks requested by subscriber: n, refilled on n/2 of received items*/
  private static final int PREFETCH_WINDOW_SIZE = 4;
  private static final Logger logger = LoggerFactory.getLogger(ResumeFileTransfer.class);

  public static void main(String[] args) {

    Resume resume = new Resume()
            .sessionDuration(Duration.ofMinutes(5))
            .retry(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))
                    .doBeforeRetry(s -> logger.debug("Disconnected. Trying to resume...")));

    RequestCodec codec = new RequestCodec();

    CloseableChannel server = RemotingServer.create(ChannelAcceptor.forRequestStream(payload -> {
              Request request = codec.decode(payload);
              payload.release();
              String fileName = request.getFileName();
              int chunkSize = request.getChunkSize();

              Flux<Long> ticks = Flux.interval(Duration.ofMillis(500)).onBackpressureDrop();

              return Files.fileSource(fileName, chunkSize)
                      .map(DefaultPayload::create)
                      .zipWith(ticks, (p, tick) -> p)
                      .log("server");
            }))
            .resume(resume)
            .bindNow(TcpServerTransport.create("localhost", 8000));

    Channel client =
            ChannelConnector.create()
                    .resume(resume)
                    .connect(TcpClientTransport.create("localhost", 8001))
                    .block();

    client.requestStream(codec.encode(new Request(16, "lorem.txt")))
            .log("client")
            .doFinally(s -> server.dispose())
            .subscribe(Files.fileSink("today-remoting/build/lorem_output.txt", PREFETCH_WINDOW_SIZE));

    server.onClose().block();
  }

  private static class RequestCodec {

    public Payload encode(Request request) {
      String encoded = request.getChunkSize() + ":" + request.getFileName();
      return DefaultPayload.create(encoded);
    }

    public Request decode(Payload payload) {
      String encoded = payload.getDataUtf8();
      String[] chunkSizeAndFileName = encoded.split(":");
      int chunkSize = Integer.parseInt(chunkSizeAndFileName[0]);
      String fileName = chunkSizeAndFileName[1];
      return new Request(chunkSize, fileName);
    }
  }

  private static class Request {
    private final int chunkSize;
    private final String fileName;

    public Request(int chunkSize, String fileName) {
      this.chunkSize = chunkSize;
      this.fileName = fileName;
    }

    public int getChunkSize() {
      return chunkSize;
    }

    public String getFileName() {
      return fileName;
    }
  }
}
