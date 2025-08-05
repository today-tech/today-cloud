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

package infra.remoting.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import infra.remoting.buffer.LeaksTrackingByteBufAllocator;
import infra.remoting.frame.ErrorFrameCodec;
import infra.remoting.frame.KeepAliveFrameCodec;
import infra.remoting.frame.LeaseFrameCodec;
import infra.remoting.frame.MetadataPushFrameCodec;
import infra.remoting.plugins.InitializingInterceptorRegistry;
import infra.remoting.test.util.TestConnection;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientServerInputMultiplexerTest {
  private TestConnection source;
  private ClientServerInputMultiplexer clientMultiplexer;
  private LeaksTrackingByteBufAllocator allocator =
          LeaksTrackingByteBufAllocator.instrument(ByteBufAllocator.DEFAULT);
  private ClientServerInputMultiplexer serverMultiplexer;

  @BeforeEach
  public void setup() {
    source = new TestConnection(allocator);
    clientMultiplexer =
            new ClientServerInputMultiplexer(source, new InitializingInterceptorRegistry(), true);
    serverMultiplexer =
            new ClientServerInputMultiplexer(source, new InitializingInterceptorRegistry(), false);
  }

  @Test
  public void clientSplits() {
    AtomicInteger clientFrames = new AtomicInteger();
    AtomicInteger serverFrames = new AtomicInteger();

    clientMultiplexer
            .asClientConnection()
            .receive()
            .doOnNext(
                    f -> {
                      clientFrames.incrementAndGet();
                      f.release();
                    })
            .subscribe();
    clientMultiplexer
            .asServerConnection()
            .receive()
            .doOnNext(
                    f -> {
                      serverFrames.incrementAndGet();
                      f.release();
                    })
            .subscribe();

    source.addToReceivedBuffer(errorFrame(1).retain());
    assertThat(clientFrames.get()).isOne();
    assertThat(serverFrames.get()).isZero();

    source.addToReceivedBuffer(errorFrame(1).retain());
    assertThat(clientFrames.get()).isEqualTo(2);
    assertThat(serverFrames.get()).isZero();

    source.addToReceivedBuffer(leaseFrame().retain());
    assertThat(clientFrames.get()).isEqualTo(3);
    assertThat(serverFrames.get()).isZero();

    source.addToReceivedBuffer(keepAliveFrame().retain());
    assertThat(clientFrames.get()).isEqualTo(4);
    assertThat(serverFrames.get()).isZero();

    source.addToReceivedBuffer(errorFrame(2).retain());
    assertThat(clientFrames.get()).isEqualTo(4);
    assertThat(serverFrames.get()).isOne();

    source.addToReceivedBuffer(errorFrame(0).retain());
    assertThat(clientFrames.get()).isEqualTo(5);
    assertThat(serverFrames.get()).isOne();

    source.addToReceivedBuffer(metadataPushFrame().retain());
    assertThat(clientFrames.get()).isEqualTo(5);
    assertThat(serverFrames.get()).isEqualTo(2);
  }

  @Test
  public void serverSplits() {
    AtomicInteger clientFrames = new AtomicInteger();
    AtomicInteger serverFrames = new AtomicInteger();

    serverMultiplexer
            .asClientConnection()
            .receive()
            .doOnNext(
                    f -> {
                      clientFrames.incrementAndGet();
                      f.release();
                    })
            .subscribe();
    serverMultiplexer
            .asServerConnection()
            .receive()
            .doOnNext(
                    f -> {
                      serverFrames.incrementAndGet();
                      f.release();
                    })
            .subscribe();

    source.addToReceivedBuffer(errorFrame(1).retain());
    assertThat(clientFrames.get()).isEqualTo(1);
    assertThat(serverFrames.get()).isZero();

    source.addToReceivedBuffer(errorFrame(1).retain());
    assertThat(clientFrames.get()).isEqualTo(2);
    assertThat(serverFrames.get()).isZero();

    source.addToReceivedBuffer(leaseFrame().retain());
    assertThat(clientFrames.get()).isEqualTo(2);
    assertThat(serverFrames.get()).isOne();

    source.addToReceivedBuffer(keepAliveFrame().retain());
    assertThat(clientFrames.get()).isEqualTo(2);
    assertThat(serverFrames.get()).isEqualTo(2);

    source.addToReceivedBuffer(errorFrame(2).retain());
    assertThat(clientFrames.get()).isEqualTo(2);
    assertThat(serverFrames.get()).isEqualTo(3);

    source.addToReceivedBuffer(errorFrame(0).retain());
    assertThat(clientFrames.get()).isEqualTo(2);
    assertThat(serverFrames.get()).isEqualTo(4);

    source.addToReceivedBuffer(metadataPushFrame().retain());
    assertThat(clientFrames.get()).isEqualTo(3);
    assertThat(serverFrames.get()).isEqualTo(4);
  }

  private ByteBuf leaseFrame() {
    return LeaseFrameCodec.encode(allocator, 1_000, 1, Unpooled.EMPTY_BUFFER);
  }

  private ByteBuf errorFrame(int i) {
    return ErrorFrameCodec.encode(allocator, i, new Exception());
  }

  private ByteBuf keepAliveFrame() {
    return KeepAliveFrameCodec.encode(allocator, false, 0, Unpooled.EMPTY_BUFFER);
  }

  private ByteBuf metadataPushFrame() {
    return MetadataPushFrameCodec.encode(allocator, Unpooled.EMPTY_BUFFER);
  }
}
