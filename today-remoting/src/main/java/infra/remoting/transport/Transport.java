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

package infra.remoting.transport;

import infra.remoting.DuplexConnection;

import static infra.remoting.frame.FrameLengthCodec.FRAME_LENGTH_MASK;

public interface Transport {

  /**
   * Configurations that exposes the maximum frame size that a {@link DuplexConnection} can bring up.
   *
   * <p>This number should not exist the 16,777,215 (maximum frame size specified by protocol spec)
   *
   * @return return maximum configured frame size limit
   */
  default int getMaxFrameLength() {
    return FRAME_LENGTH_MASK;
  }

}
