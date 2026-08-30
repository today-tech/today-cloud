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

package infra.remoting.resume;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.remoting.FrameAssert;
import infra.remoting.frame.FrameType;
import infra.remoting.frame.KeepAliveFrameCodec;
import infra.remoting.frame.ResumeFrameCodec;
import infra.remoting.keepalive.KeepAliveSupport;
import infra.remoting.test.util.TestClientTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerChannelSessionTests {

  @Test
  void sessionTimeoutSmokeTest() {
    final VirtualTimeScheduler virtualTimeScheduler = VirtualTimeScheduler.getOrSet();
    try {
      final TestClientTransport transport = new TestClientTransport();
      final InMemoryResumableFramesStore framesStore =
              new InMemoryResumableFramesStore("test", Unpooled.EMPTY_BUFFER, 100);

      transport.connect().subscribe();

      final ResumableConnection resumableConnection =
              new ResumableConnection(
                      "test", Unpooled.EMPTY_BUFFER, transport.testConnection(), framesStore);

      resumableConnection.receive().subscribe();

      final ServerChannelSession session =
              new ServerChannelSession(
                      Unpooled.EMPTY_BUFFER,
                      resumableConnection,
                      transport.testConnection(),
                      framesStore,
                      Duration.ofMinutes(1),
                      true);

      final KeepAliveSupport.ClientKeepAliveSupport keepAliveSupport =
              new KeepAliveSupport.ClientKeepAliveSupport(transport.alloc(), 1000000, 10000000);
      session.setKeepAliveSupport(keepAliveSupport);

      // connection is active. just advance time
      virtualTimeScheduler.advanceTimeBy(Duration.ofSeconds(10));
      assertThat(session.s).isNull();
      assertThat(session.isDisposed()).isFalse();

      // deactivate connection
      transport.testConnection().dispose();
      assertThat(transport.testConnection().isDisposed()).isTrue();
      // ensures timeout has been started
      assertThat(session.s).isNotNull();
      assertThat(session.isDisposed()).isFalse();

      // resubscribe so a new connection is generated
      transport.connect().subscribe();

      assertThat(transport.testConnection().isDisposed()).isFalse();
      // timeout should be still active since no RESUME_Ok frame has been received yet
      assertThat(session.s).isNotNull();
      assertThat(session.isDisposed()).isFalse();

      // advance time
      virtualTimeScheduler.advanceTimeBy(Duration.ofSeconds(50));
      // timeout should not terminate current connection
      assertThat(transport.testConnection().isDisposed()).isFalse();

      // send RESUME frame
      final ByteBuf resumeFrame =
              ResumeFrameCodec.encode(transport.alloc(), Unpooled.EMPTY_BUFFER, 0, 0);
      session.resumeWith(resumeFrame, transport.testConnection());
      resumeFrame.release();

      assertThat(transport.testConnection().isDisposed()).isFalse();
      // timeout should be terminated
      assertThat(session.s).isNull();
      assertThat(session.isDisposed()).isFalse();
      FrameAssert.assertThat(transport.testConnection().pollFrame())
              .hasStreamIdZero()
              .typeOf(FrameType.RESUME_OK)
              .matches(ReferenceCounted::release);

      virtualTimeScheduler.advanceTimeBy(Duration.ofSeconds(15));

      // disconnects for the second time
      transport.testConnection().dispose();
      assertThat(transport.testConnection().isDisposed()).isTrue();
      // ensures timeout has been started
      assertThat(session.s).isNotNull();
      assertThat(session.isDisposed()).isFalse();

      transport.connect().subscribe();

      assertThat(transport.testConnection().isDisposed()).isFalse();
      // timeout should be still active since no RESUME_Ok frame has been received yet
      assertThat(session.s).isNotNull();
      assertThat(session.isDisposed()).isFalse();

      // advance time
      virtualTimeScheduler.advanceTimeBy(Duration.ofSeconds(61));

      final ByteBuf resumeFrame1 =
              ResumeFrameCodec.encode(transport.alloc(), Unpooled.EMPTY_BUFFER, 0, 0);
      session.resumeWith(resumeFrame1, transport.testConnection());
      resumeFrame1.release();

      // should obtain new connection
      assertThat(transport.testConnection().isDisposed()).isTrue();
      // timeout should be still active since no RESUME_OK frame has been received yet
      assertThat(session.s).isEqualTo(Operators.cancelledSubscription());
      assertThat(session.isDisposed()).isTrue();

      FrameAssert.assertThat(transport.testConnection().pollFrame())
              .hasStreamIdZero()
              .typeOf(FrameType.ERROR)
              .matches(ReferenceCounted::release);

      resumableConnection.onClose().as(StepVerifier::create).expectComplete().verify();
      transport.alloc().assertHasNoLeaks();
    }
    finally {
      VirtualTimeScheduler.reset();
    }
  }

  @Test
  void shouldTerminateConnectionOnIllegalStateInKeepAliveFrame() {
    final VirtualTimeScheduler virtualTimeScheduler = VirtualTimeScheduler.getOrSet();
    try {
      final TestClientTransport transport = new TestClientTransport();
      final InMemoryResumableFramesStore framesStore =
              new InMemoryResumableFramesStore("test", Unpooled.EMPTY_BUFFER, 100);

      transport.connect().subscribe();

      final ResumableConnection resumableConnection =
              new ResumableConnection(
                      "test", Unpooled.EMPTY_BUFFER, transport.testConnection(), framesStore);

      resumableConnection.receive().subscribe();

      final ServerChannelSession session =
              new ServerChannelSession(
                      Unpooled.EMPTY_BUFFER,
                      resumableConnection,
                      transport.testConnection(),
                      framesStore,
                      Duration.ofMinutes(1),
                      true);

      final KeepAliveSupport.ClientKeepAliveSupport keepAliveSupport =
              new KeepAliveSupport.ClientKeepAliveSupport(transport.alloc(), 1000000, 10000000);
      keepAliveSupport.resumeState(session);
      session.setKeepAliveSupport(keepAliveSupport);

      // connection is active. just advance time
      virtualTimeScheduler.advanceTimeBy(Duration.ofSeconds(10));
      assertThat(session.s).isNull();
      assertThat(session.isDisposed()).isFalse();

      final ByteBuf keepAliveFrame =
              KeepAliveFrameCodec.encode(transport.alloc(), false, 1529, Unpooled.EMPTY_BUFFER);
      keepAliveSupport.receive(keepAliveFrame);
      keepAliveFrame.release();

      assertThat(transport.testConnection().isDisposed()).isTrue();
      // timeout should be terminated
      assertThat(session.s).isNotNull();
      assertThat(session.isDisposed()).isTrue();

      FrameAssert.assertThat(transport.testConnection().pollFrame())
              .hasStreamIdZero()
              .typeOf(FrameType.ERROR)
              .matches(ReferenceCounted::release);

      resumableConnection.onClose().as(StepVerifier::create).expectError().verify();
      keepAliveSupport.dispose();
      transport.alloc().assertHasNoLeaks();
    }
    finally {
      VirtualTimeScheduler.reset();
    }
  }
}
