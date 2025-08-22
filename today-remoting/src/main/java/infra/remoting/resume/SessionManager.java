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

package infra.remoting.resume;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.lang.Nullable;
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
