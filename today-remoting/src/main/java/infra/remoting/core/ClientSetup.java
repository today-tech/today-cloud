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

package infra.remoting.core;

import java.nio.channels.ClosedChannelException;

import infra.remoting.Connection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

abstract class ClientSetup {

  abstract Mono<Tuple2<ByteBuf, Connection>> init(Connection connection);
}

class DefaultClientSetup extends ClientSetup {

  @Override
  Mono<Tuple2<ByteBuf, Connection>> init(Connection connection) {
    return Mono.create(sink -> sink.onRequest(__ -> sink.success(Tuples.of(Unpooled.EMPTY_BUFFER, connection))));
  }
}

class ResumableClientSetup extends ClientSetup {

  @Override
  Mono<Tuple2<ByteBuf, Connection>> init(Connection connection) {
    return Mono.create(sink -> {
      sink.onRequest(__ -> new SetupHandlingConnection(connection, sink));

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
