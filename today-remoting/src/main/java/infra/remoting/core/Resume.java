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

import java.time.Duration;
import java.util.Objects;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.remoting.resume.InMemoryResumableFramesStore;
import infra.remoting.resume.InMemoryResumableFramesStoreFactory;
import infra.remoting.resume.RandomUUIDResumeTokenGenerator;
import infra.remoting.resume.ResumableFramesStoreFactory;
import infra.remoting.resume.ResumeTokenGenerator;
import reactor.util.retry.Retry;

/**
 * Simple holder of configuration settings for the protocol Resume capability. This can be used to
 * configure an {@link ChannelConnector} or an {@link RemotingServer} except for {@link
 * #retry(Retry)} and {@link #token(ResumeTokenGenerator)} which apply only to the client side.
 */
public class Resume {

  private static final Logger logger = LoggerFactory.getLogger(Resume.class);

  Duration sessionDuration = Duration.ofMinutes(2);

  @Nullable
  private ResumableFramesStoreFactory storeFactory;

  /* Storage */
  boolean cleanupStoreOnKeepAlive;

  Duration streamTimeout = Duration.ofSeconds(10);

  /* Client only */
  ResumeTokenGenerator tokenGenerator = new RandomUUIDResumeTokenGenerator();

  Retry retry = Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
          .maxBackoff(Duration.ofSeconds(16))
          .jitter(1.0)
          .doBeforeRetry(signal -> logger.debug("Connection error", signal.failure()));

  public Resume() {
  }

  /**
   * The maximum time for a client to keep trying to reconnect. During this time client and server
   * continue to store unsent frames to keep the session warm and ready to resume.
   *
   * <p>By default this is set to 2 minutes.
   *
   * @param sessionDuration the max duration for a session
   * @return the same instance for method chaining
   */
  public Resume sessionDuration(Duration sessionDuration) {
    this.sessionDuration = Objects.requireNonNull(sessionDuration);
    return this;
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
  public Resume cleanupStoreOnKeepAlive() {
    this.cleanupStoreOnKeepAlive = true;
    return this;
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
  public Resume cleanupStoreOnKeepAlive(boolean cleanupStoreOnKeepAlive) {
    this.cleanupStoreOnKeepAlive = cleanupStoreOnKeepAlive;
    return this;
  }

  /**
   * Configure a factory to create the storage for buffering (or persisting) a window of frames that
   * may need to be sent again to resume after a dropped connection.
   *
   * <p>By default {@link InMemoryResumableFramesStore} is used with its cache size set to 100,000
   * bytes. When the cache fills up, the oldest frames are gradually removed to create space for new
   * ones.
   *
   * @param storeFactory the factory to use to create the store
   * @return the same instance for method chaining
   */
  public Resume storeFactory(@Nullable ResumableFramesStoreFactory storeFactory) {
    this.storeFactory = storeFactory;
    return this;
  }

  /**
   * A {@link reactor.core.publisher.Flux#timeout(Duration) timeout} value to apply to the resumed
   * session stream obtained from the {@link #storeFactory(ResumableFramesStoreFactory) store} after a reconnect. The
   * resume stream must not take longer than the specified time to emit each frame.
   *
   * <p>By default, this is set to 10 seconds.
   *
   * @param streamTimeout the timeout value for resuming a session stream
   * @return the same instance for method chaining
   */
  public Resume streamTimeout(Duration streamTimeout) {
    this.streamTimeout = Objects.requireNonNull(streamTimeout);
    return this;
  }

  /**
   * Configure the logic for reconnecting. This setting is for use with {@link
   * ChannelConnector#resume(Resume)} on the client side only.
   *
   * <p>By default this is set to:
   *
   * <pre>{@code
   * Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
   *     .maxBackoff(Duration.ofSeconds(16))
   *     .jitter(1.0)
   * }</pre>
   *
   * @param retry the {@code Retry} spec to use when attempting to reconnect
   * @return the same instance for method chaining
   */
  public Resume retry(Retry retry) {
    this.retry = retry;
    return this;
  }

  /**
   * Customize the generation of the resume identification token used to resume. This setting is for
   * use with {@link ChannelConnector#resume(Resume)} on the client side only.
   *
   * <p>By default, this is {@code ResumeFrameFlyweight::generateResumeToken}.
   *
   * @param generator a custom generator for a resume identification token
   * @return the same instance for method chaining
   */
  public Resume token(ResumeTokenGenerator generator) {
    this.tokenGenerator = generator;
    return this;
  }

  // Package private accessors

  ResumableFramesStoreFactory getStoreFactory(String tag) {
    return storeFactory != null
            ? storeFactory
            : new InMemoryResumableFramesStoreFactory(tag, 100_000);
  }

}
