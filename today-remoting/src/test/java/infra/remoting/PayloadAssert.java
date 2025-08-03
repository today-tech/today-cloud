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

package infra.remoting;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;
import org.assertj.core.internal.Objects;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import infra.remoting.frame.ByteBufRepresentation;
import infra.remoting.util.DefaultPayload;

import static org.assertj.core.error.ShouldBeEqual.shouldBeEqual;
import static org.assertj.core.error.ShouldHave.shouldHave;

public class PayloadAssert extends AbstractAssert<PayloadAssert, Payload> {

  public static PayloadAssert assertThat(@Nullable Payload payload) {
    return new PayloadAssert(payload);
  }

  private final Failures failures = Failures.instance();

  public PayloadAssert(@Nullable Payload payload) {
    super(payload, PayloadAssert.class);
  }

  public PayloadAssert hasMetadata() {
    assertValid();

    if (!actual.hasMetadata()) {
      throw failures.failure(info, shouldHave(actual, new Condition<>("metadata present")));
    }

    return this;
  }

  public PayloadAssert hasNoMetadata() {
    assertValid();

    if (actual.hasMetadata()) {
      throw failures.failure(info, shouldHave(actual, new Condition<>("metadata absent")));
    }

    return this;
  }

  public PayloadAssert hasMetadata(String metadata, Charset charset) {
    return hasMetadata(metadata.getBytes(charset));
  }

  public PayloadAssert hasMetadata(String metadataUtf8) {
    return hasMetadata(metadataUtf8, CharsetUtil.UTF_8);
  }

  public PayloadAssert hasMetadata(byte[] metadata) {
    return hasMetadata(Unpooled.wrappedBuffer(metadata));
  }

  public PayloadAssert hasMetadata(ByteBuf metadata) {
    hasMetadata();

    ByteBuf content = actual.sliceMetadata();
    if (!ByteBufUtil.equals(content, metadata)) {
      throw failures.failure(info, shouldBeEqual(content, metadata, new ByteBufRepresentation()));
    }

    return this;
  }

  public PayloadAssert hasData(String dataUtf8) {
    return hasData(dataUtf8, CharsetUtil.UTF_8);
  }

  public PayloadAssert hasData(String data, Charset charset) {
    return hasData(data.getBytes(charset));
  }

  public PayloadAssert hasData(byte[] data) {
    return hasData(Unpooled.wrappedBuffer(data));
  }

  public PayloadAssert hasData(ByteBuf data) {
    assertValid();

    ByteBuf content = actual.sliceData();
    if (!ByteBufUtil.equals(content, data)) {
      throw failures.failure(info, shouldBeEqual(content, data, new ByteBufRepresentation()));
    }

    return this;
  }

  public void hasNoLeaks() {
    if (!(actual instanceof DefaultPayload)) {
      if (actual.refCnt() == 0) {
        throw failures.failure(
                info,
                new BasicErrorMessageFactory(
                        "%nExpecting:  %n<%s>   %nto have refCnt(0) after release but "
                                + "actual was already released",
                        actual, actual.refCnt()));
      }
      if (!actual.release() || actual.refCnt() > 0) {
        throw failures.failure(
                info,
                new BasicErrorMessageFactory(
                        "%nExpecting:  %n<%s>   %nto have refCnt(0) after release but "
                                + "actual was "
                                + "%n<refCnt(%s)>",
                        actual, actual.refCnt()));
      }
    }
  }

  public void isReleased() {
    if (actual.refCnt() > 0) {
      throw failures.failure(
              info,
              new BasicErrorMessageFactory(
                      "%nExpecting:  %n<%s>   %nto have refCnt(0) but " + "actual was " + "%n<refCnt(%s)>",
                      actual, actual.refCnt()));
    }
  }

  @Override
  public PayloadAssert isEqualTo(Object expected) {
    if (expected instanceof Payload) {
      if (expected == actual) {
        return this;
      }

      Payload expectedPayload = (Payload) expected;
      List<String> failedExpectation = new ArrayList<>();
      if (expectedPayload.hasMetadata() != actual.hasMetadata()) {
        failedExpectation.add(
                String.format(
                        "hasMetadata(%s) but actual was hasMetadata(%s)%n",
                        expectedPayload.hasMetadata(), actual.hasMetadata()));
      }
      else {
        if (!ByteBufUtil.equals(expectedPayload.sliceMetadata(), actual.sliceMetadata())) {
          failedExpectation.add(
                  String.format(
                          "metadata(%s) but actual was metadata(%s)%n",
                          expectedPayload.sliceMetadata(), actual.sliceMetadata()));
        }
      }

      if (!ByteBufUtil.equals(expectedPayload.sliceData(), actual.sliceData())) {
        failedExpectation.add(
                String.format(
                        "data(%s) but actual was data(%s)%n",
                        expectedPayload.sliceData(), actual.sliceData()));
      }

      if (!failedExpectation.isEmpty()) {
        throw failures.failure(
                info,
                new BasicErrorMessageFactory(
                        "%nExpecting be equal to the given one but the following differences were found"
                                + " %s",
                        failedExpectation));
      }

      return this;
    }

    return super.isEqualTo(expected);
  }

  private void assertValid() {
    Objects.instance().assertNotNull(info, actual);
  }
}
