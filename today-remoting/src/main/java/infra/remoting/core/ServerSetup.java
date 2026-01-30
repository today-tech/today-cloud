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
import java.time.Duration;
import java.util.function.BiFunction;

import infra.remoting.Connection;
import infra.remoting.ProtocolErrorException;
import infra.remoting.error.RejectedResumeException;
import infra.remoting.error.UnsupportedSetupException;
import infra.remoting.frame.ResumeFrameCodec;
import infra.remoting.frame.SetupFrameCodec;
import infra.remoting.keepalive.KeepAliveHandler;
import infra.remoting.resume.ResumableConnection;
import infra.remoting.resume.ResumableFramesStoreFactory;
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
    private final Duration resumeStreamTimeout;

    private final SessionManager sessionManager;

    private final Duration resumeSessionDuration;

    private final boolean cleanupStoreOnKeepAlive;

    private final ResumableFramesStoreFactory resumeStoreFactory;

    ResumableServerSetup(Duration timeout, SessionManager sessionManager,
            Duration resumeSessionDuration, Duration resumeStreamTimeout,
            ResumableFramesStoreFactory resumeStoreFactory, boolean cleanupStoreOnKeepAlive) {
      super(timeout);
      this.sessionManager = sessionManager;
      this.resumeStoreFactory = resumeStoreFactory;
      this.resumeStreamTimeout = resumeStreamTimeout;
      this.resumeSessionDuration = resumeSessionDuration;
      this.cleanupStoreOnKeepAlive = cleanupStoreOnKeepAlive;
    }

    @Override
    public Mono<Void> acceptChannelSetup(ByteBuf frame, Connection connection, BiFunction<KeepAliveHandler, Connection, Mono<Void>> then) {
      if (SetupFrameCodec.resumeEnabled(frame)) {
        ByteBuf resumeToken = SetupFrameCodec.resumeToken(frame);

        var resumableFramesStore = resumeStoreFactory.create(resumeToken);
        var resumableConnection = new ResumableConnection("server", resumeToken, connection, resumableFramesStore);
        var serverChannelSession = new ServerChannelSession(resumeToken, resumableConnection, connection,
                resumableFramesStore, resumeSessionDuration, cleanupStoreOnKeepAlive);

        sessionManager.save(serverChannelSession, resumeToken);

        return then.apply(new ResumableKeepAliveHandler(resumableConnection, serverChannelSession, serverChannelSession), resumableConnection);
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
      }
      else {
        sendError(connection, new RejectedResumeException("unknown resume token"));
      }
      return connection.onClose();
    }

    @Override
    public void dispose() {
      sessionManager.dispose();
    }
  }
}
