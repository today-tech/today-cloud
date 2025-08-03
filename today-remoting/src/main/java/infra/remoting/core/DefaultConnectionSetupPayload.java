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

import infra.remoting.ConnectionSetupPayload;
import infra.remoting.frame.FrameHeaderCodec;
import infra.remoting.frame.SetupFrameCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Default implementation of {@link ConnectionSetupPayload}. Primarily for internal use within
 * RSocket Java but may be created in an application, e.g. for testing purposes.
 */
public class DefaultConnectionSetupPayload extends ConnectionSetupPayload {

  private final ByteBuf setupFrame;

  public DefaultConnectionSetupPayload(ByteBuf setupFrame) {
    this.setupFrame = setupFrame;
  }

  @Override
  public boolean hasMetadata() {
    return FrameHeaderCodec.hasMetadata(setupFrame);
  }

  @Override
  public ByteBuf sliceMetadata() {
    final ByteBuf metadata = SetupFrameCodec.metadata(setupFrame);
    return metadata == null ? Unpooled.EMPTY_BUFFER : metadata;
  }

  @Override
  public ByteBuf sliceData() {
    return SetupFrameCodec.data(setupFrame);
  }

  @Override
  public ByteBuf data() {
    return sliceData();
  }

  @Override
  public ByteBuf metadata() {
    return sliceMetadata();
  }

  @Override
  public String metadataMimeType() {
    return SetupFrameCodec.metadataMimeType(setupFrame);
  }

  @Override
  public String dataMimeType() {
    return SetupFrameCodec.dataMimeType(setupFrame);
  }

  @Override
  public int keepAliveInterval() {
    return SetupFrameCodec.keepAliveInterval(setupFrame);
  }

  @Override
  public int keepAliveMaxLifetime() {
    return SetupFrameCodec.keepAliveMaxLifetime(setupFrame);
  }

  @Override
  public int getFlags() {
    return FrameHeaderCodec.flags(setupFrame);
  }

  @Override
  public boolean willClientHonorLease() {
    return SetupFrameCodec.honorLease(setupFrame);
  }

  @Override
  public boolean isResumeEnabled() {
    return SetupFrameCodec.resumeEnabled(setupFrame);
  }

  @Override
  public ByteBuf resumeToken() {
    return SetupFrameCodec.resumeToken(setupFrame);
  }

  @Override
  public ConnectionSetupPayload touch() {
    setupFrame.touch();
    return this;
  }

  @Override
  public ConnectionSetupPayload touch(Object hint) {
    setupFrame.touch(hint);
    return this;
  }

  @Override
  protected void deallocate() {
    setupFrame.release();
  }
}
