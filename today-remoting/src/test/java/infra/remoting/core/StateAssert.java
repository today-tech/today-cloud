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

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Failures;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static infra.remoting.core.ShouldHaveFlag.shouldHaveFlag;
import static infra.remoting.core.ShouldHaveFlag.shouldHaveRequestN;
import static infra.remoting.core.ShouldHaveFlag.shouldHaveRequestNBetween;
import static infra.remoting.core.ShouldNotHaveFlag.shouldNotHaveFlag;
import static infra.remoting.core.StateUtils.FIRST_FRAME_SENT_FLAG;
import static infra.remoting.core.StateUtils.INBOUND_TERMINATED_FLAG;
import static infra.remoting.core.StateUtils.OUTBOUND_TERMINATED_FLAG;
import static infra.remoting.core.StateUtils.REASSEMBLING_FLAG;
import static infra.remoting.core.StateUtils.SUBSCRIBED_FLAG;
import static infra.remoting.core.StateUtils.TERMINATED_STATE;
import static infra.remoting.core.StateUtils.UNSUBSCRIBED_STATE;
import static infra.remoting.core.StateUtils.extractRequestN;
import static infra.remoting.core.StateUtils.isFirstFrameSent;
import static infra.remoting.core.StateUtils.isReassembling;
import static infra.remoting.core.StateUtils.isSubscribed;

public class StateAssert<T> extends AbstractAssert<StateAssert<T>, AtomicLongFieldUpdater<T>> {

  public static <T> StateAssert<T> assertThat(AtomicLongFieldUpdater<T> updater, T instance) {
    return new StateAssert<>(updater, instance);
  }

  public static StateAssert<FireAndForgetRequesterMono> assertThat(
          FireAndForgetRequesterMono instance) {
    return new StateAssert<>(FireAndForgetRequesterMono.STATE, instance);
  }

  public static StateAssert<RequestResponseRequesterMono> assertThat(
          RequestResponseRequesterMono instance) {
    return new StateAssert<>(RequestResponseRequesterMono.STATE, instance);
  }

  public static StateAssert<RequestStreamRequesterFlux> assertThat(
          RequestStreamRequesterFlux instance) {
    return new StateAssert<>(RequestStreamRequesterFlux.STATE, instance);
  }

  public static StateAssert<RequestChannelRequesterFlux> assertThat(
          RequestChannelRequesterFlux instance) {
    return new StateAssert<>(RequestChannelRequesterFlux.STATE, instance);
  }

  public static StateAssert<RequestChannelResponderSubscriber> assertThat(
          RequestChannelResponderSubscriber instance) {
    return new StateAssert<>(RequestChannelResponderSubscriber.STATE, instance);
  }

  private final Failures failures = Failures.instance();
  private final T instance;

  public StateAssert(AtomicLongFieldUpdater<T> updater, T instance) {
    super(updater, StateAssert.class);
    this.instance = instance;
  }

  public StateAssert<T> isUnsubscribed() {
    long currentState = actual.get(instance);
    if (isSubscribed(currentState) || StateUtils.isTerminated(currentState)) {
      throw failures.failure(info, shouldHaveFlag(currentState, UNSUBSCRIBED_STATE));
    }
    return this;
  }

  public StateAssert<T> hasSubscribedFlagOnly() {
    long currentState = actual.get(instance);
    if (currentState != SUBSCRIBED_FLAG) {
      throw failures.failure(info, shouldHaveFlag(currentState, SUBSCRIBED_FLAG));
    }
    return this;
  }

  public StateAssert<T> hasSubscribedFlag() {
    long currentState = actual.get(instance);
    if (!isSubscribed(currentState)) {
      throw failures.failure(info, shouldHaveFlag(currentState, SUBSCRIBED_FLAG));
    }
    return this;
  }

  public StateAssert<T> hasRequestN(long n) {
    long currentState = actual.get(instance);
    if (extractRequestN(currentState) != n) {
      throw failures.failure(info, shouldHaveRequestN(currentState, n));
    }
    return this;
  }

  public StateAssert<T> hasRequestNBetween(long min, long max) {
    long currentState = actual.get(instance);
    final long requestN = extractRequestN(currentState);
    if (requestN < min || requestN > max) {
      throw failures.failure(info, shouldHaveRequestNBetween(currentState, min, max));
    }
    return this;
  }

  public StateAssert<T> hasFirstFrameSentFlag() {
    long currentState = actual.get(instance);
    if (!isFirstFrameSent(currentState)) {
      throw failures.failure(info, shouldHaveFlag(currentState, FIRST_FRAME_SENT_FLAG));
    }
    return this;
  }

  public StateAssert<T> hasNoFirstFrameSentFlag() {
    long currentState = actual.get(instance);
    if (isFirstFrameSent(currentState)) {
      throw failures.failure(info, shouldNotHaveFlag(currentState, FIRST_FRAME_SENT_FLAG));
    }
    return this;
  }

  public StateAssert<T> hasReassemblingFlag() {
    long currentState = actual.get(instance);
    if (!isReassembling(currentState)) {
      throw failures.failure(info, shouldHaveFlag(currentState, REASSEMBLING_FLAG));
    }
    return this;
  }

  public StateAssert<T> hasNoReassemblingFlag() {
    long currentState = actual.get(instance);
    if (isReassembling(currentState)) {
      throw failures.failure(info, shouldNotHaveFlag(currentState, REASSEMBLING_FLAG));
    }
    return this;
  }

  public StateAssert<T> hasInboundTerminated() {
    long currentState = actual.get(instance);
    if (!StateUtils.isInboundTerminated(currentState)) {
      throw failures.failure(info, shouldHaveFlag(currentState, INBOUND_TERMINATED_FLAG));
    }
    return this;
  }

  public StateAssert<T> hasOutboundTerminated() {
    long currentState = actual.get(instance);
    if (!StateUtils.isOutboundTerminated(currentState)) {
      throw failures.failure(info, shouldHaveFlag(currentState, OUTBOUND_TERMINATED_FLAG));
    }
    return this;
  }

  public StateAssert<T> isTerminated() {
    long currentState = actual.get(instance);
    if (!StateUtils.isTerminated(currentState)) {
      throw failures.failure(info, shouldHaveFlag(currentState, TERMINATED_STATE));
    }
    return this;
  }
}
