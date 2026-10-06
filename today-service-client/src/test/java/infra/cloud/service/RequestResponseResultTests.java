package infra.cloud.service;

import org.junit.jupiter.api.Test;

import infra.cloud.service.serialize.ResponseDeserializer;
import infra.remoting.Payload;
import infra.remoting.util.ByteBufPayload;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for request-response result decoding and resource ownership.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/4
 */
class RequestResponseResultTests {

  @Test
  void releasesPayloadForValueAndNull() {
    verifyDecodedValue("value");
    verifyDecodedValue(null);
  }

  private void verifyDecodedValue(Object value) {
    var method = mock(ServiceInterfaceMethod.class);
    var deserializer = mock(ResponseDeserializer.class);
    Payload payload = ByteBufPayload.create(Unpooled.buffer().writeByte(0));
    when(deserializer.deserialize(method, payload.data())).thenReturn(value);
    var result = new RequestResponseResult(method, Mono.just(payload), deserializer);
    if (value == null) {
      StepVerifier.create(InvocationResults.publisher(result)).verifyComplete();
    }
    else {
      StepVerifier.create(InvocationResults.publisher(result)).expectNext(value).verifyComplete();
    }
    assertThat(payload.refCnt()).isZero();
  }

  @Test
  void releasesPayloadAndRecordsDecodingFailure() {
    var method = mock(ServiceInterfaceMethod.class);
    var deserializer = mock(ResponseDeserializer.class);
    Payload payload = ByteBufPayload.create(Unpooled.buffer().writeByte(0));
    var error = new IllegalStateException("invalid response");
    when(deserializer.deserialize(method, payload.data())).thenThrow(error);
    var result = new RequestResponseResult(method, Mono.just(payload), deserializer);
    StepVerifier.create(InvocationResults.publisher(result)).expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error)).verify();
    assertThat(payload.refCnt()).isZero();
    assertThat(result.isFailed()).isTrue();
    assertThat(result.getException()).isSameAs(error);
  }
}
