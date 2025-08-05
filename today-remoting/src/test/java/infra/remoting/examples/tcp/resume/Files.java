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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import infra.remoting.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

class Files {
  private static final Logger logger = LoggerFactory.getLogger(Files.class);

  public static Flux<ByteBuf> fileSource(String fileName, int chunkSizeBytes) {
    return Flux.generate(
            () -> new FileState(fileName, chunkSizeBytes), FileState::consumeNext, FileState::dispose);
  }

  public static Subscriber<Payload> fileSink(String fileName, int windowSize) {
    return new Subscriber<Payload>() {
      Subscription s;
      int requests = windowSize;
      OutputStream outputStream;
      int receivedBytes;
      int receivedCount;

      @Override
      public void onSubscribe(Subscription s) {
        this.s = s;
        this.s.request(requests);
      }

      @Override
      public void onNext(Payload payload) {
        ByteBuf data = payload.data();
        receivedBytes += data.readableBytes();
        receivedCount += 1;
        logger.debug("Received file chunk: " + receivedCount + ". Total size: " + receivedBytes);
        if (outputStream == null) {
          outputStream = open(fileName);
        }
        write(outputStream, data);
        payload.release();

        requests--;
        if (requests == windowSize / 2) {
          requests += windowSize;
          s.request(windowSize);
        }
      }

      private void write(OutputStream outputStream, ByteBuf byteBuf) {
        try {
          byteBuf.readBytes(outputStream, byteBuf.readableBytes());
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void onError(Throwable t) {
        close(outputStream);
      }

      @Override
      public void onComplete() {
        close(outputStream);
      }

      private OutputStream open(String filename) {
        try {
          /*do not buffer for demo purposes*/
          return new FileOutputStream(filename);
        }
        catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      private void close(OutputStream stream) {
        if (stream != null) {
          try {
            stream.close();
          }
          catch (IOException e) {
          }
        }
      }
    };
  }

  private static class FileState {
    private final String fileName;
    private final int chunkSizeBytes;
    private BufferedInputStream inputStream;
    private byte[] chunkBytes;

    public FileState(String fileName, int chunkSizeBytes) {
      this.fileName = fileName;
      this.chunkSizeBytes = chunkSizeBytes;
    }

    public FileState consumeNext(SynchronousSink<ByteBuf> sink) {
      if (inputStream == null) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);
        if (in == null) {
          sink.error(new FileNotFoundException(fileName));
          return this;
        }
        this.inputStream = new BufferedInputStream(in);
        this.chunkBytes = new byte[chunkSizeBytes];
      }
      try {
        int consumedBytes = inputStream.read(chunkBytes);
        if (consumedBytes == -1) {
          sink.complete();
        }
        else {
          sink.next(Unpooled.copiedBuffer(chunkBytes, 0, consumedBytes));
        }
      }
      catch (IOException e) {
        sink.error(e);
      }
      return this;
    }

    public void dispose() {
      if (inputStream != null) {
        try {
          inputStream.close();
        }
        catch (IOException e) {
        }
      }
    }
  }
}
