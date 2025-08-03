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

package io.rsocket.core;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketErrorException;
import io.rsocket.exceptions.RejectedResumeException;
import io.rsocket.exceptions.UnsupportedSetupException;
import io.rsocket.frame.ResumeFrameCodec;
import io.rsocket.frame.SetupFrameCodec;
import io.rsocket.keepalive.KeepAliveHandler;
import io.rsocket.resume.ResumableDuplexConnection;
import io.rsocket.resume.ResumableFramesStore;
import io.rsocket.resume.ServerRSocketSession;
import io.rsocket.resume.SessionManager;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static io.rsocket.keepalive.KeepAliveHandler.DefaultKeepAliveHandler;
import static io.rsocket.keepalive.KeepAliveHandler.ResumableKeepAliveHandler;

abstract class ServerSetup {

  final Duration timeout;

  protected ServerSetup(Duration timeout) {
    this.timeout = timeout;
  }

  Mono<Tuple2<ByteBuf, DuplexConnection>> init(DuplexConnection connection) {
    return Mono.<Tuple2<ByteBuf, DuplexConnection>>create(sink -> sink.onRequest(__ -> new SetupHandlingDuplexConnection(connection, sink)))
            .timeout(this.timeout)
            .or(connection.onClose().then(Mono.error(ClosedChannelException::new)));
  }

  abstract Mono<Void> acceptRSocketSetup(ByteBuf frame,
          DuplexConnection clientServerConnection,
          BiFunction<KeepAliveHandler, DuplexConnection, Mono<Void>> then);

  abstract Mono<Void> acceptRSocketResume(ByteBuf frame, DuplexConnection connection);

  void dispose() { }

  void sendError(DuplexConnection duplexConnection, RSocketErrorException exception) {
    duplexConnection.sendErrorAndClose(exception);
    duplexConnection.receive().subscribe();
  }

  static class DefaultServerSetup extends ServerSetup {

    DefaultServerSetup(Duration timeout) {
      super(timeout);
    }

    @Override
    public Mono<Void> acceptRSocketSetup(ByteBuf frame, DuplexConnection duplexConnection,
            BiFunction<KeepAliveHandler, DuplexConnection, Mono<Void>> then) {

      if (SetupFrameCodec.resumeEnabled(frame)) {
        sendError(duplexConnection, new UnsupportedSetupException("resume not supported"));
        return duplexConnection.onClose();
      }
      else {
        return then.apply(new DefaultKeepAliveHandler(), duplexConnection);
      }
    }

    @Override
    public Mono<Void> acceptRSocketResume(ByteBuf frame, DuplexConnection duplexConnection) {
      sendError(duplexConnection, new RejectedResumeException("resume not supported"));
      return duplexConnection.onClose();
    }
  }

  static class ResumableServerSetup extends ServerSetup {
    private final SessionManager sessionManager;
    private final Duration resumeSessionDuration;
    private final Duration resumeStreamTimeout;
    private final Function<? super ByteBuf, ? extends ResumableFramesStore> resumeStoreFactory;
    private final boolean cleanupStoreOnKeepAlive;

    ResumableServerSetup(Duration timeout, SessionManager sessionManager,
            Duration resumeSessionDuration, Duration resumeStreamTimeout,
            Function<? super ByteBuf, ? extends ResumableFramesStore> resumeStoreFactory, boolean cleanupStoreOnKeepAlive) {
      super(timeout);
      this.sessionManager = sessionManager;
      this.resumeSessionDuration = resumeSessionDuration;
      this.resumeStreamTimeout = resumeStreamTimeout;
      this.resumeStoreFactory = resumeStoreFactory;
      this.cleanupStoreOnKeepAlive = cleanupStoreOnKeepAlive;
    }

    @Override
    public Mono<Void> acceptRSocketSetup(ByteBuf frame, DuplexConnection duplexConnection,
            BiFunction<KeepAliveHandler, DuplexConnection, Mono<Void>> then) {

      if (SetupFrameCodec.resumeEnabled(frame)) {
        ByteBuf resumeToken = SetupFrameCodec.resumeToken(frame);

        final ResumableFramesStore resumableFramesStore = resumeStoreFactory.apply(resumeToken);
        final ResumableDuplexConnection resumableDuplexConnection =
                new ResumableDuplexConnection(
                        "server", resumeToken, duplexConnection, resumableFramesStore);
        final ServerRSocketSession serverRSocketSession =
                new ServerRSocketSession(
                        resumeToken,
                        resumableDuplexConnection,
                        duplexConnection,
                        resumableFramesStore,
                        resumeSessionDuration,
                        cleanupStoreOnKeepAlive);

        sessionManager.save(serverRSocketSession, resumeToken);

        return then.apply(new ResumableKeepAliveHandler(
                        resumableDuplexConnection, serverRSocketSession, serverRSocketSession),
                resumableDuplexConnection);
      }
      else {
        return then.apply(new DefaultKeepAliveHandler(), duplexConnection);
      }
    }

    @Override
    public Mono<Void> acceptRSocketResume(ByteBuf frame, DuplexConnection duplexConnection) {
      ServerRSocketSession session = sessionManager.get(ResumeFrameCodec.token(frame));
      if (session != null) {
        session.resumeWith(frame, duplexConnection);
        return duplexConnection.onClose();
      }
      else {
        sendError(duplexConnection, new RejectedResumeException("unknown resume token"));
        return duplexConnection.onClose();
      }
    }

    @Override
    public void dispose() {
      sessionManager.dispose();
    }
  }
}
