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

package infra.remoting.plugins;

import org.jspecify.annotations.Nullable;

import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import infra.remoting.lease.Lease;
import io.netty.buffer.ByteBuf;
import reactor.core.Disposable;
import reactor.util.context.Context;

/**
 * Class used to track the protocol requests lifecycles. The main difference and advantage of this
 * interceptor compares to {@link ChannelDecorator} is that it allows intercepting the initial and
 * terminal phases on every individual request.
 *
 * <p><b>Note</b>, if any of the invocations will rise a runtime exception, this exception will be
 * caught and be propagated to {@link reactor.core.publisher.Operators#onErrorDropped(Throwable,
 * Context)}
 */
public interface RequestInterceptor extends Disposable {

  /**
   * Method which is being invoked on successful acceptance and start of a request.
   *
   * @param streamId used for the request
   * @param requestType of the request. Must be one of the following types {@link
   * FrameType#REQUEST_FNF}, {@link FrameType#REQUEST_RESPONSE}, {@link
   * FrameType#REQUEST_STREAM} or {@link FrameType#REQUEST_CHANNEL}
   * @param metadata taken from the initial frame
   */
  void onStart(int streamId, FrameType requestType, @Nullable ByteBuf metadata);

  /**
   * Method which is being invoked once a successfully accepted request is terminated. This method
   * can be invoked only after the {@link #onStart(int, FrameType, ByteBuf)} method. This method is
   * exclusive with {@link #onCancel(int, FrameType)}.
   *
   * @param streamId used by this request
   * @param requestType of the request. Must be one of the following types {@link
   * FrameType#REQUEST_FNF}, {@link FrameType#REQUEST_RESPONSE}, {@link
   * FrameType#REQUEST_STREAM} or {@link FrameType#REQUEST_CHANNEL}
   * @param t with which this finished has terminated. Must be one of the following signals
   */
  void onTerminate(int streamId, FrameType requestType, @Nullable Throwable t);

  /**
   * Method which is being invoked once a successfully accepted request is cancelled. This method
   * can be invoked only after the {@link #onStart(int, FrameType, ByteBuf)} method. This method is
   * exclusive with {@link #onTerminate(int, FrameType, Throwable)}.
   *
   * @param requestType of the request. Must be one of the following types {@link
   * FrameType#REQUEST_FNF}, {@link FrameType#REQUEST_RESPONSE}, {@link
   * FrameType#REQUEST_STREAM} or {@link FrameType#REQUEST_CHANNEL}
   * @param streamId used by this request
   */
  void onCancel(int streamId, FrameType requestType);

  /**
   * Method which is being invoked on the request rejection. This method is being called only if the
   * actual request can not be started and is called instead of the {@link #onStart(int, FrameType,
   * ByteBuf)} method. The reason for rejection can be one of the following:
   *
   * <p>
   *
   * <ul>
   *   <li>No available {@link Lease} on the requester or the responder sides
   *   <li>Invalid {@link Payload} size or format on the Requester side, so the request
   *       is being rejected before the actual streamId is generated
   *   <li>A second subscription on the ongoing Request
   * </ul>
   *
   * @param rejectionReason exception which causes rejection of a particular request
   * @param requestType of the request. Must be one of the following types {@link
   * FrameType#REQUEST_FNF}, {@link FrameType#REQUEST_RESPONSE}, {@link
   * FrameType#REQUEST_STREAM} or {@link FrameType#REQUEST_CHANNEL}
   * @param metadata taken from the initial frame
   */
  void onReject(Throwable rejectionReason, FrameType requestType, @Nullable ByteBuf metadata);

}
