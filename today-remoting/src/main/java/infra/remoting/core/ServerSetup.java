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
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;

import infra.remoting.Connection;
import infra.remoting.ProtocolErrorException;
import infra.remoting.error.RejectedResumeException;
import infra.remoting.error.UnsupportedSetupException;
import infra.remoting.frame.ResumeFrameCodec;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.keepalive.KeepAliveHandler;
import infra.remoting.resume.ResumableConnection;
import infra.remoting.resume.ResumableFramesStore;
import infra.remoting.resume.ServerChannelSession;
import infra.remoting.resume.SessionManager;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static infra.remoting.keepalive.KeepAliveHandler.DefaultKeepAliveHandler;
import static infra.remoting.keepalive.KeepAliveHandler.ResumableKeepAliveHandler;

abstract class ServerSetup {

  final Duration timeout;

  protected ServerSetup(Duration timeout) {
    this.timeout = timeout;
  }

  Mono<Tuple2<ByteBuf, Connection>> init(Connection connection) {
    return Mono.<Tuple2<ByteBuf, Connection>>create(sink -> sink.onRequest(__ -> new SetupHandlingConnection(connection, sink)))
            .timeout(this.timeout)
            .or(connection.onClose().then(Mono.error(ClosedChannelException::new)));
  }

  abstract Mono<Void> acceptChannelSetup(ByteBuf frame,
          Connection clientServerConnection,
          BiFunction<KeepAliveHandler, Connection, Mono<Void>> then);

  abstract Mono<Void> acceptChannelResume(ByteBuf frame, Connection connection);

  void dispose() { }

  void sendError(Connection connection, ProtocolErrorException exception) {
    connection.sendErrorAndClose(exception);
    connection.receive().subscribe();
  }

  static class DefaultServerSetup extends ServerSetup {

    DefaultServerSetup(Duration timeout) {
      super(timeout);
    }

    @Override
    public Mono<Void> acceptChannelSetup(ByteBuf frame, Connection connection,
            BiFunction<KeepAliveHandler, Connection, Mono<Void>> then) {

      if (SetupFrameCodec.resumeEnabled(frame)) {
        sendError(connection, new UnsupportedSetupException("resume not supported"));
        return connection.onClose();
      }
      else {
        return then.apply(new DefaultKeepAliveHandler(), connection);
      }
    }

    @Override
    public Mono<Void> acceptChannelResume(ByteBuf frame, Connection connection) {
      sendError(connection, new RejectedResumeException("resume not supported"));
      return connection.onClose();
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
    public Mono<Void> acceptChannelSetup(ByteBuf frame, Connection connection,
            BiFunction<KeepAliveHandler, Connection, Mono<Void>> then) {

      if (SetupFrameCodec.resumeEnabled(frame)) {
        ByteBuf resumeToken = SetupFrameCodec.resumeToken(frame);

        final ResumableFramesStore resumableFramesStore = resumeStoreFactory.apply(resumeToken);
        final ResumableConnection resumableConnection =
                new ResumableConnection(
                        "server", resumeToken, connection, resumableFramesStore);
        final ServerChannelSession serverChannelSession =
                new ServerChannelSession(
                        resumeToken,
                        resumableConnection,
                        connection,
                        resumableFramesStore,
                        resumeSessionDuration,
                        cleanupStoreOnKeepAlive);

        sessionManager.save(serverChannelSession, resumeToken);

        return then.apply(new ResumableKeepAliveHandler(
                        resumableConnection, serverChannelSession, serverChannelSession),
                resumableConnection);
      }
      else {
        return then.apply(new DefaultKeepAliveHandler(), connection);
      }
    }

    @Override
    public Mono<Void> acceptChannelResume(ByteBuf frame, Connection connection) {
      ServerChannelSession session = sessionManager.get(ResumeFrameCodec.token(frame));
      if (session != null) {
        session.resumeWith(frame, connection);
        return connection.onClose();
      }
      else {
        sendError(connection, new RejectedResumeException("unknown resume token"));
        return connection.onClose();
      }
    }

    @Override
    public void dispose() {
      sessionManager.dispose();
    }
  }
}
