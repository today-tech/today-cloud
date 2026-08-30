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

package infra.remoting.core;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import io.netty.util.ReferenceCounted;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendUtilsTests {

  @Test
  void droppedElementsConsumerShouldAcceptOtherTypesThanReferenceCounted() {
    Consumer value = extractDroppedElementConsumer();
    value.accept(new Object());
  }

  @Test
  void droppedElementsConsumerReleaseReference() {
    ReferenceCounted referenceCounted = mock(ReferenceCounted.class);
    when(referenceCounted.release()).thenReturn(true);

    Consumer value = extractDroppedElementConsumer();
    value.accept(referenceCounted);

    verify(referenceCounted).release();
  }

  private static Consumer<?> extractDroppedElementConsumer() {
    return (Consumer<?>) SendUtils.DISCARD_CONTEXT.stream().findAny().get().getValue();
  }
}
