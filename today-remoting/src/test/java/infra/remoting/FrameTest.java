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

public class FrameTest {
  /*@Test
  public void testFrameToString() {
    final io.rsocket.Frame requestFrame =
        io.rsocket.Frame.Request.from(
            1, FrameType.REQUEST_RESPONSE, DefaultPayload.create("streaming in -> 0"), 1);
    assertEquals(
        "Frame => Stream ID: 1 Type: REQUEST_RESPONSE Payload: data: \"streaming in -> 0\" ",
        requestFrame.toString());
  }

  @Test
  public void testFrameWithMetadataToString() {
    final io.rsocket.Frame requestFrame =
        io.rsocket.Frame.Request.from(
            1,
            FrameType.REQUEST_RESPONSE,
            DefaultPayload.create("streaming in -> 0", "metadata"),
            1);
    assertEquals(
        "Frame => Stream ID: 1 Type: REQUEST_RESPONSE Payload: metadata: \"metadata\" data: \"streaming in -> 0\" ",
        requestFrame.toString());
  }

  @Test
  public void testPayload() {
    io.rsocket.Frame frame =
        io.rsocket.Frame.PayloadFrame.from(
            1,
            FrameType.NEXT_COMPLETE,
            DefaultPayload.create("Hello"),
            FrameHeaderFlyweight.FLAGS_C);
    frame.toString();
  }*/
}
