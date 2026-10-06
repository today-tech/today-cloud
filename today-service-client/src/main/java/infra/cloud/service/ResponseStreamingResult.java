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

package infra.cloud.service;

import org.reactivestreams.Publisher;

import infra.cloud.service.serialize.ResponseDeserializer;
import infra.remoting.Payload;
import infra.util.concurrent.Future;
import reactor.core.publisher.Flux;

/**
 * Client-side result for a request followed by a response stream.
 *
 * <p>The data subscription drives consumption and demand. Each payload is
 * released after decoding, even on failure. Observing completion does not subscribe
 * to the source; successful completion, errors, and cancellation update the same
 * result lifecycle. Decoded null values are not emitted as stream elements.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/15 20:34
 */
final class ResponseStreamingResult implements StreamingInvocationResult {

  private final StreamingInvocationResult delegate;

  ResponseStreamingResult(ServiceInterfaceMethod method, Flux<Payload> source, ResponseDeserializer decoder) {
    delegate = InvocationResults.stream(InvocationType.RESPONSE_STREAMING, source.handle((payload, sink) -> {
      try {
        Object value = decoder.deserialize(method, payload.data());
        if (value != null) {
          sink.next(value);
        }
      }
      finally {
        payload.release();
      }
    }));
  }

  public InvocationType getType() {
    return delegate.getType();
  }

  public Future<Void> completion() {
    return delegate.completion();
  }

  public void start() {
    delegate.start();
  }

  public boolean cancel() {
    return delegate.cancel();
  }

  public Publisher<Object> values() {
    return delegate.values();
  }
}
