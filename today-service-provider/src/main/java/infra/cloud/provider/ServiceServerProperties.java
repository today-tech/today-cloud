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

package infra.cloud.provider;

import org.jspecify.annotations.Nullable;

import java.time.Duration;

import infra.cloud.service.config.ResumeProperties;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.NestedConfigurationProperty;
import infra.remoting.frame.FrameLengthCodec;
import infra.util.DataSize;

/**
 * Service provider server properties
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/9/4 16:56
 */
@ConfigurationProperties("today.service.server")
public class ServiceServerProperties {

  /**
   * The address to bind
   */
  @Nullable
  private String bindAddress;

  /**
   * The port to bind
   */
  private int port = 9000;

  /**
   * Configurations that exposes the maximum frame size that a Connection can bring up.
   */
  private DataSize maxFrameLength = DataSize.ofBytes(FrameLengthCodec.FRAME_LENGTH_MASK);

  /**
   * The threshold size for reassembly, must not be less than 64 bytes
   */
  private DataSize maxInboundPayloadSize = DataSize.ofBytes(Integer.MAX_VALUE);

  /**
   * Specify the max time to wait for the first frame (e.g. {@code SETUP})
   * on an accepted connection.
   */
  private Duration maxTimeToFirstFrame = Duration.ofMinutes(1);

  /**
   * Protocol frames larger than the given maximum transmission unit (mtu) size value are
   * fragmented. the threshold size for fragmentation, must be no less than 64
   */
  private DataSize maxTransmissionUnit = DataSize.ofBytes(0);

  @NestedConfigurationProperty
  public final ResumeProperties resume = new ResumeProperties();

  public void setPort(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }

  /**
   * The address to bind to
   *
   * @param bindAddress the address to bind to
   */
  public void setBindAddress(@Nullable String bindAddress) {
    this.bindAddress = bindAddress;
  }

  @Nullable
  public String getBindAddress() {
    return bindAddress;
  }

  /**
   * When this is set, frames reassembler control maximum payload size which can be reassembled.
   *
   * <p>By default, this is not set in which case maximum reassembled payloads size is not
   * controlled.
   *
   * @param maxInboundPayloadSize the threshold size for reassembly, must not be less than 64 bytes.
   * Please note, {@code maxInboundPayloadSize} must always be greater or equal to {@link
   * infra.remoting.transport.Transport#getMaxFrameLength()}, otherwise inbound frame can exceed the
   * {@code maxInboundPayloadSize}
   */
  public void setMaxInboundPayloadSize(DataSize maxInboundPayloadSize) {
    this.maxInboundPayloadSize = maxInboundPayloadSize;
  }

  public DataSize getMaxInboundPayloadSize() {
    return maxInboundPayloadSize;
  }

  /**
   * Configurations that exposes the maximum frame size that a {@link infra.remoting.Connection} can bring up.
   *
   * <p>This number should not exist the 16,777,215 (maximum frame size specified by protocol spec)
   */
  public void setMaxFrameLength(DataSize maxFrameLength) {
    this.maxFrameLength = maxFrameLength;
  }

  /**
   * @return return maximum configured frame size limit
   */
  public DataSize getMaxFrameLength() {
    return maxFrameLength;
  }

  public void setMaxTimeToFirstFrame(Duration maxTimeToFirstFrame) {
    this.maxTimeToFirstFrame = maxTimeToFirstFrame;
  }

  public Duration getMaxTimeToFirstFrame() {
    return maxTimeToFirstFrame;
  }

  public DataSize getMaxTransmissionUnit() {
    return maxTransmissionUnit;
  }

  public void setMaxTransmissionUnit(DataSize maxTransmissionUnit) {
    this.maxTransmissionUnit = maxTransmissionUnit;
  }
}
