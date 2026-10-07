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

package infra.remoting.resume;

import infra.remoting.Closeable;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Store for resumable frames
 */
public interface ResumableFramesStore extends Closeable {

  /**
   * Save resumable frames for potential resumption
   *
   * @param frames {@link Flux} of resumable frames
   * @return {@link Mono} which completes once all resume frames are written
   */
  Mono<Void> saveFrames(Flux<ByteBuf> frames);

  /**
   * Release frames from tail of the store up to remote implied position
   */
  void releaseFrames(long remoteImpliedPos);

  /**
   * @return {@link Flux} of frames from store tail to head. It should terminate with error if
   * frames are not continuous
   */
  Flux<ByteBuf> resumeStream();

  /**
   * @return Local frame position as defined by protocol
   */
  long framePosition();

  /**
   * @return Implied frame position as defined by protocol
   */
  long frameImpliedPosition();

  /**
   * Received resumable frame as defined by protocol. Implementation must increment frame
   * implied position
   *
   * @return {@code true} if information about the frame has been stored
   */
  boolean resumableFrameReceived(ByteBuf frame);

}
