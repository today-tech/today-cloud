package infra.cloud.provider;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import infra.cloud.serialize.ReturnValueSerializer;
import infra.cloud.serialize.Writable;
import infra.cloud.service.ServiceMethod;
import infra.test.util.ReflectionTestUtils;
import infra.remoting.Payload;
import infra.remoting.util.ByteBufPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;
import java.time.Duration;

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
      public boolean supportsReturnValue(ServiceMethod candidate) { return candidate == method; }

      public Mono<Object> handleReturnValue(RemoteRequest candidate, Object result) {
        assertThat(candidate).isSameAs(request);
        seen.set(result);
        return Mono.just("adapted");
      }
    };
    var handler = new ServiceChannelHandler(mock(LocalServiceHolder.class), deserializer, serializer, List.of(custom));
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
  void malformedRequestReleasesPayload() {
    var deserializer = mock(RequestDeserializer.class);
    when(deserializer.deserialize(any())).thenThrow(new IllegalArgumentException("invalid request"));
    var handler = new ServiceChannelHandler(mock(LocalServiceHolder.class), deserializer, mock(ResponseSerializer.class));
    Payload payload = ByteBufPayload.create(Unpooled.buffer().writeByte(0));
    StepVerifier.create(handler.requestResponse(payload)).expectError(IllegalArgumentException.class).verify();
    assertThat(payload.refCnt()).isZero();
  }

  @Test
  void encodingFailureReleasesBuffer() {
    var buffer = new AtomicReference<ByteBuf>();
    var serializer = new ReturnValueSerializer<Object>() {
      public boolean supportsReturnValue(ServiceMethod method) { return true; }

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
