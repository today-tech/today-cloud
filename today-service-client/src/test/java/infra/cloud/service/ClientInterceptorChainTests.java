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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Client chain ordering, short circuit and context replacement tests.
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/6
 */
class ClientInterceptorChainTests {
  @Test
  void orderedChainIsReusableAndSharesCallContext() throws Throwable {
    var events = new ArrayList<String>();
    var result = mock(InvocationResult.class);
    ClientInterceptor first = (invocation, chain) -> {
      assertThat(invocation.getAttribute("key")).isNull();
      invocation.setAttribute("key", "shared");
      invocation.getMetadata().add("tenant-id", "tenant");
      events.add("first-before");
      var value = chain.proceed(invocation);
      events.add("first-after");
      return value;
    };
    ClientInterceptor second = (invocation, chain) -> {
      assertThat(invocation.getAttribute("key")).isEqualTo("shared");
      events.add("second-before");
      var value = chain.proceed(invocation);
      events.add("second-after");
      return value;
    };
    var list = new ArrayList<>(List.of(first, second));
    var chain = new DefaultInterceptorChain(list, invocation -> {
      assertThat(invocation.getMetadata().get("tenant-id")).isEqualTo("tenant");
      events.add("terminal");
      return result;
    });
    list.clear();
    for (int i = 0; i < 2; i++) {
      events.clear();
      assertThat(chain.proceed(new DefaultClientRequest(mock(ServiceInterfaceMethod.class), new Object[0])))
              .isSameAs(result);
      assertThat(events).containsExactly("first-before", "second-before", "terminal", "second-after", "first-after");
    }
  }

  @Test
  void shortCircuitSkipsTerminal() throws Throwable {
    var calls = new AtomicInteger();
    var result = mock(InvocationResult.class);
    var chain = new DefaultInterceptorChain(List.of((invocation, next) -> result), invocation -> {
      calls.incrementAndGet();
      return result;
    });
    assertThat(chain.proceed(mock(ClientRequest.class))).isSameAs(result);
    assertThat(calls).hasValue(0);
  }

  @Test
  void replacementContextReachesTerminalAndExceptionIsPreserved() {
    var replacement = mock(ClientRequest.class);
    var failure = new IllegalStateException("failed");
    var chain = new DefaultInterceptorChain(List.of((invocation, next) -> next.proceed(replacement)), invocation -> {
      assertThat(invocation).isSameAs(replacement);
      throw failure;
    });
    assertThatThrownBy(() -> chain.proceed(mock(ClientRequest.class))).isSameAs(failure);
  }
}
