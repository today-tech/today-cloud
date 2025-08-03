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

package io.rsocket.plugins;

import java.util.List;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.rsocket.frame.FrameType;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

class CompositeRequestInterceptor implements RequestInterceptor {

  final RequestInterceptor[] requestInterceptors;

  CompositeRequestInterceptor(RequestInterceptor[] requestInterceptors) {
    this.requestInterceptors = requestInterceptors;
  }

  @Override
  public void dispose() {
    final RequestInterceptor[] requestInterceptors = this.requestInterceptors;
    for (final RequestInterceptor requestInterceptor : requestInterceptors) {
      requestInterceptor.dispose();
    }
  }

  @Override
  public void onStart(int streamId, FrameType requestType, @Nullable ByteBuf metadata) {
    final RequestInterceptor[] requestInterceptors = this.requestInterceptors;
    for (final RequestInterceptor requestInterceptor : requestInterceptors) {
      try {
        requestInterceptor.onStart(streamId, requestType, metadata);
      }
      catch (Throwable t) {
        Operators.onErrorDropped(t, Context.empty());
      }
    }
  }

  @Override
  public void onTerminate(int streamId, FrameType requestType, @Nullable Throwable cause) {
    final RequestInterceptor[] requestInterceptors = this.requestInterceptors;
    for (final RequestInterceptor requestInterceptor : requestInterceptors) {
      try {
        requestInterceptor.onTerminate(streamId, requestType, cause);
      }
      catch (Throwable t) {
        Operators.onErrorDropped(t, Context.empty());
      }
    }
  }

  @Override
  public void onCancel(int streamId, FrameType requestType) {
    final RequestInterceptor[] requestInterceptors = this.requestInterceptors;
    for (final RequestInterceptor requestInterceptor : requestInterceptors) {
      try {
        requestInterceptor.onCancel(streamId, requestType);
      }
      catch (Throwable t) {
        Operators.onErrorDropped(t, Context.empty());
      }
    }
  }

  @Override
  public void onReject(Throwable rejectionReason, FrameType requestType, @Nullable ByteBuf metadata) {
    final RequestInterceptor[] requestInterceptors = this.requestInterceptors;
    for (final RequestInterceptor requestInterceptor : requestInterceptors) {
      try {
        requestInterceptor.onReject(rejectionReason, requestType, metadata);
      }
      catch (Throwable t) {
        Operators.onErrorDropped(t, Context.empty());
      }
    }
  }

  @Nullable
  static RequestInterceptor create(List<RequestInterceptor> interceptors) {
    return switch (interceptors.size()) {
      case 0 -> null;
      case 1 -> new SafeRequestInterceptor(interceptors.get(0));
      default -> new CompositeRequestInterceptor(interceptors.toArray(new RequestInterceptor[0]));
    };
  }

  static class SafeRequestInterceptor implements RequestInterceptor {

    final RequestInterceptor requestInterceptor;

    public SafeRequestInterceptor(RequestInterceptor requestInterceptor) {
      this.requestInterceptor = requestInterceptor;
    }

    @Override
    public void dispose() {
      requestInterceptor.dispose();
    }

    @Override
    public boolean isDisposed() {
      return requestInterceptor.isDisposed();
    }

    @Override
    public void onStart(int streamId, FrameType requestType, @Nullable ByteBuf metadata) {
      try {
        requestInterceptor.onStart(streamId, requestType, metadata);
      }
      catch (Throwable t) {
        Operators.onErrorDropped(t, Context.empty());
      }
    }

    @Override
    public void onTerminate(int streamId, FrameType requestType, @Nullable Throwable cause) {
      try {
        requestInterceptor.onTerminate(streamId, requestType, cause);
      }
      catch (Throwable t) {
        Operators.onErrorDropped(t, Context.empty());
      }
    }

    @Override
    public void onCancel(int streamId, FrameType requestType) {
      try {
        requestInterceptor.onCancel(streamId, requestType);
      }
      catch (Throwable t) {
        Operators.onErrorDropped(t, Context.empty());
      }
    }

    @Override
    public void onReject(
            Throwable rejectionReason, FrameType requestType, @Nullable ByteBuf metadata) {
      try {
        requestInterceptor.onReject(rejectionReason, requestType, metadata);
      }
      catch (Throwable t) {
        Operators.onErrorDropped(t, Context.empty());
      }
    }
  }
}
