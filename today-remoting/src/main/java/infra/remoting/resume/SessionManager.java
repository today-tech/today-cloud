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

package infra.remoting.resume;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class SessionManager {
  static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

  private volatile boolean isDisposed;
  private final Map<String, ServerChannelSession> sessions = new ConcurrentHashMap<>();

  public ServerChannelSession save(ServerChannelSession session, ByteBuf resumeToken) {
    if (isDisposed) {
      session.dispose();
    }
    else {
      final String token = resumeToken.toString(CharsetUtil.UTF_8);
      session.resumableConnection.onClose().doFinally(__ -> {
                logger.debug("ResumableConnection has been closed. Removing associated session '{}'", token);
                if (isDisposed || sessions.get(token) == session) {
                  sessions.remove(token);
                }
              })
              .subscribe();
      ServerChannelSession prevSession = sessions.remove(token);
      if (prevSession != null) {
        prevSession.dispose();
      }
      sessions.put(token, session);
    }
    return session;
  }

  @Nullable
  public ServerChannelSession get(ByteBuf resumeToken) {
    return sessions.get(resumeToken.toString(CharsetUtil.UTF_8));
  }

  public void dispose() {
    isDisposed = true;
    sessions.values().forEach(ServerChannelSession::dispose);
  }
}
