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

import infra.remoting.Closeable;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Store for resumable frames */
public interface ResumableFramesStore extends Closeable {

  /**
   * Save resumable frames for potential resumption
   *
   * @param frames {@link Flux} of resumable frames
   * @return {@link Mono} which completes once all resume frames are written
   */
  Mono<Void> saveFrames(Flux<ByteBuf> frames);

  /** Release frames from tail of the store up to remote implied position */
  void releaseFrames(long remoteImpliedPos);

  /**
   * @return {@link Flux} of frames from store tail to head. It should terminate with error if
   * frames are not continuous
   */
  Flux<ByteBuf> resumeStream();

  /** @return Local frame position as defined by protocol */
  long framePosition();

  /** @return Implied frame position as defined by protocol */
  long frameImpliedPosition();

  /**
   * Received resumable frame as defined by protocol. Implementation must increment frame
   * implied position
   *
   * @return {@code true} if information about the frame has been stored
   */
  boolean resumableFrameReceived(ByteBuf frame);
}
