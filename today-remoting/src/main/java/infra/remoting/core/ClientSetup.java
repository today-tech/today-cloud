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

import java.nio.channels.ClosedChannelException;

import infra.remoting.DuplexConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

abstract class ClientSetup {

  abstract Mono<Tuple2<ByteBuf, DuplexConnection>> init(DuplexConnection connection);
}

class DefaultClientSetup extends ClientSetup {

  @Override
  Mono<Tuple2<ByteBuf, DuplexConnection>> init(DuplexConnection connection) {
    return Mono.create(sink -> sink.onRequest(__ -> sink.success(Tuples.of(Unpooled.EMPTY_BUFFER, connection))));
  }
}

class ResumableClientSetup extends ClientSetup {

  @Override
  Mono<Tuple2<ByteBuf, DuplexConnection>> init(DuplexConnection connection) {
    return Mono.create(sink -> {
      sink.onRequest(__ -> new SetupHandlingDuplexConnection(connection, sink));

      Disposable subscribe = connection.onClose()
              .doFinally(__ -> sink.error(new ClosedChannelException()))
              .subscribe();
      sink.onCancel(() -> {
        subscribe.dispose();
        connection.dispose();
        connection.receive().subscribe();
      });
    });
  }
}
