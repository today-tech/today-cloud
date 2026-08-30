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

import java.util.List;

import infra.remoting.frame.FrameType;
import io.netty.buffer.ByteBuf;
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
