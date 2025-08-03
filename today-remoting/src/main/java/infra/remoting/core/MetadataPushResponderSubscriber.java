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

import org.reactivestreams.Subscription;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import reactor.core.CoreSubscriber;

final class MetadataPushResponderSubscriber implements CoreSubscriber<Void> {
  static final Logger logger = LoggerFactory.getLogger(MetadataPushResponderSubscriber.class);

  static final MetadataPushResponderSubscriber INSTANCE = new MetadataPushResponderSubscriber();

  private MetadataPushResponderSubscriber() { }

  @Override
  public void onSubscribe(Subscription s) {
    s.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(Void voidVal) { }

  @Override
  public void onError(Throwable t) {
    logger.debug("Dropped error", t);
  }

  @Override
  public void onComplete() { }
}
