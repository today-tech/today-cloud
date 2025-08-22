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

package infra.remoting.keepalive;

import java.util.function.Consumer;

import infra.remoting.keepalive.KeepAliveSupport.KeepAlive;
import infra.remoting.resume.ChannelSession;
import infra.remoting.resume.ResumableConnection;
import infra.remoting.resume.ResumeStateHolder;
import io.netty.buffer.ByteBuf;

public interface KeepAliveHandler {

  KeepAliveFramesAcceptor start(KeepAliveSupport keepAliveSupport,
          Consumer<ByteBuf> onFrameSent, Consumer<KeepAlive> onTimeout);

  class DefaultKeepAliveHandler implements KeepAliveHandler {

    @Override
    public KeepAliveFramesAcceptor start(KeepAliveSupport keepAliveSupport,
            Consumer<ByteBuf> onSendKeepAliveFrame, Consumer<KeepAlive> onTimeout) {
      return keepAliveSupport
              .onSendKeepAliveFrame(onSendKeepAliveFrame)
              .onTimeout(onTimeout)
              .start();
    }
  }

  class ResumableKeepAliveHandler implements KeepAliveHandler {

    private final ResumableConnection resumableConnection;

    private final ChannelSession channelSession;

    private final ResumeStateHolder resumeStateHolder;

    public ResumableKeepAliveHandler(ResumableConnection resumableConnection,
            ChannelSession channelSession, ResumeStateHolder resumeStateHolder) {
      this.resumableConnection = resumableConnection;
      this.channelSession = channelSession;
      this.resumeStateHolder = resumeStateHolder;
    }

    @Override
    public KeepAliveFramesAcceptor start(KeepAliveSupport keepAliveSupport,
            Consumer<ByteBuf> onSendKeepAliveFrame, Consumer<KeepAlive> onTimeout) {
      channelSession.setKeepAliveSupport(keepAliveSupport);

      return keepAliveSupport
              .resumeState(resumeStateHolder)
              .onSendKeepAliveFrame(onSendKeepAliveFrame)
              .onTimeout(keepAlive -> resumableConnection.disconnect())
              .start();
    }
  }
}
