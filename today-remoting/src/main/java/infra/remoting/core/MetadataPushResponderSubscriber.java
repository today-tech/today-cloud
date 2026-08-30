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
