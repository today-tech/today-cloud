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

package infra.cloud.provider;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import infra.cloud.serialize.ReturnValueSerializer;
import infra.cloud.serialize.Writable;
import infra.cloud.service.ServiceMethod;
import infra.remoting.Payload;
import infra.remoting.util.ByteBufPayload;
import infra.test.util.ReflectionTestUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for request-response resource ownership and return value handlers.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/4
 */
class RequestResponseResourceTests {

  @Test
  void customHandlerTakesPrecedenceAndReceivesReturnValue() throws Throwable {
    var deserializer = mock(RequestDeserializer.class);
    var request = mock(RemoteRequest.class);
    var method = mock(InvocableMethod.class);
    var serializer = mock(ResponseSerializer.class);
    Object value = new Object();
    when(request.getMethod()).thenReturn(method);
    when(request.invoke()).thenReturn(value);
    when(deserializer.deserialize(any())).thenReturn(request);
    when(serializer.serialize(request, "adapted"))
            .thenAnswer(invocation -> Mono.just(ByteBufPayload.create(Unpooled.buffer().writeByte(42))));
    var seen = new AtomicReference<Object>();
    var custom = new ReturnValueHandler() {
      public boolean supportsReturnValue(ServiceMethod candidate) {
        return candidate == method;
      }

      public infra.cloud.service.InvocationResult handleReturnValue(RemoteRequest candidate, Object result) {
        assertThat(candidate).isSameAs(request);
        seen.set(result);
        return infra.cloud.service.InvocationResults.success("adapted");
      }
    };
    when(method.handleReturnValue(request, value)).thenAnswer(invocation -> custom.handleReturnValue(request, value));
    var handler = new ServiceChannelHandler(deserializer, serializer, new DefaultServiceRequestExecutor());
    Payload payload = ByteBufPayload.create(Unpooled.buffer().writeByte(0));
    StepVerifier.create(handler.requestResponse(payload)).assertNext(response -> {
      try {
        assertThat(response.data().readByte()).isEqualTo((byte) 42);
      }
      finally {
        response.release();
      }
    }).expectComplete().verify(Duration.ofSeconds(5));
    assertThat(seen.get()).isSameAs(value);
    assertThat(payload.refCnt()).isZero();
  }

  @Test
  void transportCanDelegateToAnIndependentExecutor() {
    var deserializer = mock(RequestDeserializer.class);
    var request = mock(RemoteRequest.class);
    var serializer = mock(ResponseSerializer.class);
    when(deserializer.deserialize(any())).thenReturn(request);
    when(serializer.serialize(request, "external"))
            .thenAnswer(invocation -> Mono.just(ByteBufPayload.create(Unpooled.buffer().writeByte(7))));
    ServiceRequestExecutor executor = candidate -> {
      assertThat(candidate).isSameAs(request);
      return infra.cloud.service.InvocationResults.success("external");
    };
    var handler = new ServiceChannelHandler(deserializer, serializer, executor);
    Payload payload = ByteBufPayload.create(Unpooled.buffer().writeByte(0));
    StepVerifier.create(handler.requestResponse(payload)).assertNext(response -> {
      try {
        assertThat(response.data().readByte()).isEqualTo((byte) 7);
      }
      finally {
        response.release();
      }
    }).verifyComplete();
    assertThat(payload.refCnt()).isZero();
  }

  @Test
  void malformedMetadataReleasesBothBuffers() {
    var handler = new ServiceChannelHandler(mock(RequestDeserializer.class), mock(ResponseSerializer.class),
            request -> infra.cloud.service.InvocationResults.success(null));
    var data = Unpooled.buffer().writeByte(0);
    var metadata = Unpooled.buffer().writeByte(99);
    Payload payload = ByteBufPayload.create(data, metadata);
    StepVerifier.create(handler.requestResponse(payload)).expectError(IllegalArgumentException.class).verify();
    assertThat(data.refCnt()).isZero();
    assertThat(metadata.refCnt()).isZero();
  }

  @Test
  void malformedRequestReleasesPayload() {
    var deserializer = mock(RequestDeserializer.class);
    when(deserializer.deserialize(any())).thenThrow(new IllegalArgumentException("invalid request"));
    var handler = new ServiceChannelHandler(deserializer, mock(ResponseSerializer.class));
    Payload payload = ByteBufPayload.create(Unpooled.buffer().writeByte(0));
    StepVerifier.create(handler.requestResponse(payload)).expectError(IllegalArgumentException.class).verify();
    assertThat(payload.refCnt()).isZero();
  }

  @Test
  void malformedStreamingAndNotificationRequestsReleasePayloads() {
    var deserializer = mock(RequestDeserializer.class);
    when(deserializer.deserialize(any())).thenThrow(new IllegalArgumentException("invalid request"));
    when(deserializer.deserializeMethod(any())).thenThrow(new IllegalArgumentException("invalid header"));
    var handler = new ServiceChannelHandler(deserializer, mock(ResponseSerializer.class));
    Payload stream = ByteBufPayload.create(Unpooled.buffer().writeByte(0));
    StepVerifier.create(handler.requestStream(stream)).expectErrorMessage("invalid request").verify();
    assertThat(stream.refCnt()).isZero();
    Payload notification = ByteBufPayload.create(Unpooled.buffer().writeByte(0));
    StepVerifier.create(handler.fireAndForget(notification)).expectErrorMessage("invalid request").verify();
    assertThat(notification.refCnt()).isZero();
    Payload header = ByteBufPayload.create(Unpooled.buffer().writeByte(0));
    StepVerifier.create(handler.requestChannel(Mono.just(header))).expectErrorMessage("invalid header").verify();
    assertThat(header.refCnt()).isZero();
    StepVerifier.create(handler.requestChannel(reactor.core.publisher.Flux.empty()))
            .expectErrorMessage("Missing channel method header").verify();
  }

  @Test
  void encodingFailureReleasesBuffer() {
    var buffer = new AtomicReference<ByteBuf>();
    var serializer = new ReturnValueSerializer<Object>() {
      public boolean supportsReturnValue(ServiceMethod method) {
        return true;
      }

      public void serialize(ServiceMethod method, Object value, Writable writable) {
        buffer.set((ByteBuf) ReflectionTestUtils.getField(writable, "buffer"));
        throw new IllegalStateException("encoding failure");
      }
    };
    var request = mock(RemoteRequest.class);
    when(request.getMethod()).thenReturn(mock(InvocableMethod.class));
    StepVerifier.create(new ResponseSerializer(List.of(serializer)).serialize(request, "value"))
            .expectError(IllegalStateException.class).verify();
    assertThat(buffer.get().refCnt()).isZero();
  }
}
