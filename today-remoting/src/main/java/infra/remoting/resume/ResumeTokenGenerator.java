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

package infra.remoting.resume;

import infra.remoting.core.ChannelConnector;
import infra.remoting.core.Resume;
import io.netty.buffer.ByteBuf;

/**
 * Generator for a resume identification token
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ChannelConnector#resume(Resume)
 * @since 1.0 2025/8/23 00:21
 */
public interface ResumeTokenGenerator {

  /**
   * Customize the generation of the resume identification token used to resume.
   * This setting is for use with {@link ChannelConnector#resume(Resume)} on the client side only.
   */
  ByteBuf generate();

}
