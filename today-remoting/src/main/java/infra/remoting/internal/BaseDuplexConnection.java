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
package infra.remoting.internal;

import infra.remoting.DuplexConnection;
import io.netty.buffer.ByteBuf;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public abstract class BaseDuplexConnection implements DuplexConnection {

  protected final Sinks.Empty<Void> onClose = Sinks.empty();

  protected final UnboundedProcessor sender = new UnboundedProcessor(onClose::tryEmitEmpty);

  public BaseDuplexConnection() {
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
