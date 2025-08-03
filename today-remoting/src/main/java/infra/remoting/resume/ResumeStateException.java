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

class ResumeStateException extends RuntimeException {
  private static final long serialVersionUID = -5393753463377588732L;
  private final long localPos;
  private final long localImpliedPos;
  private final long remotePos;
  private final long remoteImpliedPos;

  public ResumeStateException(
          long localPos, long localImpliedPos, long remotePos, long remoteImpliedPos) {
    this.localPos = localPos;
    this.localImpliedPos = localImpliedPos;
    this.remotePos = remotePos;
    this.remoteImpliedPos = remoteImpliedPos;
  }

  public long getLocalPos() {
    return localPos;
  }

  public long getLocalImpliedPos() {
    return localImpliedPos;
  }

  public long getRemotePos() {
    return remotePos;
  }

  public long getRemoteImpliedPos() {
    return remoteImpliedPos;
  }
}
