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

package infra.remoting.plugins;

import infra.lang.Nullable;
import infra.remoting.Payload;
import infra.remoting.frame.FrameType;
import infra.remoting.lease.Lease;
import io.netty.buffer.ByteBuf;
import reactor.core.Disposable;
import reactor.util.context.Context;

/**
 * Class used to track the RSocket requests lifecycles. The main difference and advantage of this
 * interceptor compares to {@link ChannelInterceptor} is that it allows intercepting the initial and
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
