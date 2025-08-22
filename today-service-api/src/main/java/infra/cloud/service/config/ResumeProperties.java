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

package infra.cloud.service.config;

import java.time.Duration;

import infra.remoting.core.Resume;
import infra.remoting.resume.ResumableFramesStoreFactory;
import infra.util.DataSize;

/**
 * Resume config properties
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Resume
 * @since 1.0 2025/8/22 21:32
 */
public class ResumeProperties {

  /**
   * Whether connection resume is enabled. Defaults to true.
   */
  private boolean enabled = true;

  /**
   * The maximum time for a client to keep trying to reconnect.
   */
  private Duration sessionDuration = Duration.ofMinutes(2);

  /**
   * A timeout value to apply to the resumed session stream obtained from the store after a reconnect.
   */
  private Duration streamTimeout = Duration.ofSeconds(10);

  /**
   * Memory cache limit bytes
   */
  private DataSize memoryCacheLimit = DataSize.ofBytes(100_000);

  private boolean cleanupStoreOnKeepAlive = false;

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * A {@link reactor.core.publisher.Flux#timeout(Duration) timeout} value to apply to the resumed
   * session stream obtained from the {@link Resume#storeFactory(ResumableFramesStoreFactory) store} after a reconnect.
   * The resume stream must not take longer than the specified time to emit each frame.
   *
   * <p>By default, this is set to 10 seconds.
   *
   * @param streamTimeout the timeout value for resuming a session stream
   */
  public void setStreamTimeout(Duration streamTimeout) {
    this.streamTimeout = streamTimeout;
  }

  public Duration getStreamTimeout() {
    return streamTimeout;
  }

  /**
   * The maximum time for a client to keep trying to reconnect. During this time client and server
   * continue to store unsent frames to keep the session warm and ready to resume.
   *
   * <p>By default, this is set to 2 minutes.
   *
   * @param sessionDuration the max duration for a session
   */
  public void setSessionDuration(Duration sessionDuration) {
    this.sessionDuration = sessionDuration;
  }

  public Duration getSessionDuration() {
    return sessionDuration;
  }

  public DataSize getMemoryCacheLimit() {
    return memoryCacheLimit;
  }

  public void setMemoryCacheLimit(DataSize memoryCacheLimit) {
    this.memoryCacheLimit = memoryCacheLimit;
  }

  /**
   * When this property is enabled, hints from {@code KEEPALIVE} frames about how much data has been
   * received by the other side, is used to proactively clean frames from the {@link
   * Resume#storeFactory(ResumableFramesStoreFactory) store}.
   *
   * <p>By default, this is set to {@code false} in which case information from {@code KEEPALIVE} is
   * ignored and old frames from the store are removed only when the store runs out of space.
   */
  public void setCleanupStoreOnKeepAlive(boolean cleanupStoreOnKeepAlive) {
    this.cleanupStoreOnKeepAlive = cleanupStoreOnKeepAlive;
  }

  /**
   * When this property is enabled, hints from {@code KEEPALIVE} frames about how much data has been
   * received by the other side, is used to proactively clean frames from the {@link
   * Resume#storeFactory(ResumableFramesStoreFactory) store}.
   *
   * <p>By default, this is set to {@code false} in which case information from {@code KEEPALIVE} is
   * ignored and old frames from the store are removed only when the store runs out of space.
   *
   * @return the same instance for method chaining
   */
  public boolean isCleanupStoreOnKeepAlive() {
    return cleanupStoreOnKeepAlive;
  }

}
