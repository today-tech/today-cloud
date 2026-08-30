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
