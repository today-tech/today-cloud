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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package infra.remoting.internal.jctools.queues;

/**
 * This interface is provided for monitoring purposes only and is only available on queues where it
 * is easy to provide it. The producer/consumer progress indicators usually correspond with the
 * number of elements offered/polled, but they are not guaranteed to maintain that semantic.
 *
 * @author nitsanw
 */
public interface QueueProgressIndicators {

  /**
   * This method has no concurrent visibility semantics. The value returned may be negative. Under
   * normal circumstances 2 consecutive calls to this method can offer an idea of progress made by
   * producer threads by subtracting the 2 results though in extreme cases (if producers have
   * progressed by more than 2^64) this may also fail.<br>
   * This value will normally indicate number of elements passed into the queue, but may under some
   * circumstances be a derivative of that figure. This method should not be used to derive size or
   * emptiness.
   *
   * @return the current value of the producer progress index
   */
  long currentProducerIndex();

  /**
   * This method has no concurrent visibility semantics. The value returned may be negative. Under
   * normal circumstances 2 consecutive calls to this method can offer an idea of progress made by
   * consumer threads by subtracting the 2 results though in extreme cases (if consumers have
   * progressed by more than 2^64) this may also fail.<br>
   * This value will normally indicate number of elements taken out of the queue, but may under some
   * circumstances be a derivative of that figure. This method should not be used to derive size or
   * emptiness.
   *
   * @return the current value of the consumer progress index
   */
  long currentConsumerIndex();
}
