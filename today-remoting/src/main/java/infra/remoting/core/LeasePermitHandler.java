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

/** Handler which enables async lease permits issuing */
interface LeasePermitHandler {

  /**
   * Called by {@link RequesterLeaseTracker} when there is an available lease
   *
   * @return {@code true} to indicate that lease permit was consumed successfully
   */
  boolean handlePermit();

  /**
   * Called by {@link RequesterLeaseTracker} when there are no lease permit available at the moment
   * and the list of awaiting {@link LeasePermitHandler} reached the configured limit
   *
   * @param t associated lease permit rejection exception
   */
  void handlePermitError(Throwable t);
}
