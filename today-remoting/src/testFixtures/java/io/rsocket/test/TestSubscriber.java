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

package io.rsocket.test;

import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.rsocket.Payload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class TestSubscriber {
  public static <T> Subscriber<T> create() {
    return create(Long.MAX_VALUE);
  }

  public static <T> Subscriber<T> create(long initialRequest) {
    @SuppressWarnings("unchecked")
    Subscriber<T> mock = mock(Subscriber.class);

    Mockito.doAnswer(
                    invocation -> {
                      if (initialRequest > 0) {
                        ((Subscription) invocation.getArguments()[0]).request(initialRequest);
                      }
                      return null;
                    })
            .when(mock)
            .onSubscribe(any(Subscription.class));

    return mock;
  }

  public static Payload anyPayload() {
    return any(Payload.class);
  }

  public static Subscriber<Payload> createCancelling() {
    @SuppressWarnings("unchecked")
    Subscriber<Payload> mock = mock(Subscriber.class);

    Mockito.doAnswer(
                    invocation -> {
                      ((Subscription) invocation.getArguments()[0]).cancel();
                      return null;
                    })
            .when(mock)
            .onSubscribe(any(Subscription.class));

    return mock;
  }
}
