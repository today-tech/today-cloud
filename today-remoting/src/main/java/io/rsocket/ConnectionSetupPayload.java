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

package io.rsocket;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;

/**
 * Exposes information from the {@code SETUP} frame to a server, as well as to client responders.
 */
public abstract class ConnectionSetupPayload extends AbstractReferenceCounted implements Payload {

  public abstract String metadataMimeType();

  public abstract String dataMimeType();

  public abstract int keepAliveInterval();

  public abstract int keepAliveMaxLifetime();

  public abstract int getFlags();

  public abstract boolean willClientHonorLease();

  public abstract boolean isResumeEnabled();

  @Nullable
  public abstract ByteBuf resumeToken();

  @Override
  public ConnectionSetupPayload retain() {
    super.retain();
    return this;
  }

  @Override
  public ConnectionSetupPayload retain(int increment) {
    super.retain(increment);
    return this;
  }

  @Override
  public abstract ConnectionSetupPayload touch();
}
