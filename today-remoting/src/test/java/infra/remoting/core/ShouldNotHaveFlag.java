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

import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.error.ErrorMessageFactory;

import java.util.HashMap;
import java.util.Map;

import static infra.remoting.core.StateUtils.REQUEST_MASK;
import static infra.remoting.core.StateUtils.SUBSCRIBED_FLAG;
import static infra.remoting.core.StateUtils.extractRequestN;

class ShouldNotHaveFlag extends BasicErrorMessageFactory {

  static final Map<Long, String> FLAGS_NAMES = new HashMap<>() {
    {
      put(StateUtils.UNSUBSCRIBED_STATE, "UNSUBSCRIBED");
      put(StateUtils.TERMINATED_STATE, "TERMINATED");
      put(SUBSCRIBED_FLAG, "SUBSCRIBED");
      put(StateUtils.REQUEST_MASK, "REQUESTED(%n)");
      put(StateUtils.FIRST_FRAME_SENT_FLAG, "FIRST_FRAME_SENT");
      put(StateUtils.REASSEMBLING_FLAG, "REASSEMBLING");
      put(StateUtils.INBOUND_TERMINATED_FLAG, "INBOUND_TERMINATED");
      put(StateUtils.OUTBOUND_TERMINATED_FLAG, "OUTBOUND_TERMINATED");
    }
  };

  static final String SHOULD_NOT_HAVE_FLAG =
          "Expected state\n\t%s\nto not have\n\t%s\nbut had\n\t[%s]";

  private ShouldNotHaveFlag(long currentState, long expectedFlag, String actualFlags) {
    super(
            SHOULD_NOT_HAVE_FLAG,
            toBinaryString(currentState),
            FLAGS_NAMES.get(expectedFlag),
            actualFlags);
  }

  static ErrorMessageFactory shouldNotHaveFlag(long currentState, long expectedFlag) {
    StringBuilder stringBuilder = new StringBuilder();
    long flag = 1L << 31;
    for (int i = 0; i < 33; i++, flag <<= 1) {
      if ((currentState & flag) == flag) {
        if (!stringBuilder.isEmpty()) {
          stringBuilder.append(", ");
        }
        stringBuilder.append(FLAGS_NAMES.get(flag));
      }
    }
    long requestN = extractRequestN(currentState);
    if (requestN > 0) {
      if (!stringBuilder.isEmpty()) {
        stringBuilder.append(", ");
      }
      stringBuilder.append(String.format(FLAGS_NAMES.get(REQUEST_MASK), requestN));
    }
    return new ShouldNotHaveFlag(currentState, expectedFlag, stringBuilder.toString());
  }

  static String toBinaryString(long state) {
    StringBuilder binaryString = new StringBuilder(Long.toBinaryString(state));

    int diff = 64 - binaryString.length();
    for (int i = 0; i < diff; i++) {
      binaryString.insert(0, "0");
    }

    binaryString.insert(33, "_");
    binaryString.insert(0, "0b");

    return binaryString.toString();
  }
}
