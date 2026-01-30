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
package infra.remoting.internal;

import infra.remoting.Connection;
import io.netty.buffer.ByteBuf;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public abstract class BaseConnection implements Connection {

  protected final Sinks.Empty<Void> onClose = Sinks.empty();

  protected final UnboundedProcessor sender = new UnboundedProcessor(onClose::tryEmitEmpty);

  public BaseConnection() {
  }

  @Override
  public void sendFrame(int streamId, ByteBuf frame) {
    if (streamId == 0) {
      sender.tryEmitPrioritized(frame);
    }
    else {
      sender.tryEmitNormal(frame);
    }
  }

  protected abstract void doOnClose();

  @Override
  public Mono<Void> onClose() {
    return onClose.asMono();
  }

  @Override
  public final void dispose() {
    doOnClose();
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public final boolean isDisposed() {
    return onClose.scan(Scannable.Attr.TERMINATED) || onClose.scan(Scannable.Attr.CANCELLED);
  }
}
