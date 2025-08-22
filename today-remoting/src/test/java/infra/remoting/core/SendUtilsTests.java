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
