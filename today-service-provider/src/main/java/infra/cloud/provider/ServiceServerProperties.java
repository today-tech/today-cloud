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

package infra.cloud.provider;

import java.time.Duration;

import infra.cloud.service.config.ResumeProperties;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.NestedConfigurationProperty;
import infra.lang.Nullable;
import infra.remoting.frame.FrameLengthCodec;
import infra.util.DataSize;

/**
 * Service provider server properties
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/9/4 16:56
 */
@ConfigurationProperties("service.server")
public class ServiceServerProperties {

  /**
   * The address to bind
   */
  @Nullable
  private String bindAddress;

  /**
   * The port to bind
   */
  private int port;

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
