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

package infra.config.etcd;

import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.core.env.PropertySource;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.util.ExceptionUtils;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/10/6 21:41
 */
public class EtcdPropertySource extends PropertySource<KV> {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private final String namespace;

  public EtcdPropertySource(KV kvClient) {
    this(kvClient, null);
  }

  public EtcdPropertySource(KV kvClient, @Nullable String namespace) {
    super("etcd", kvClient);
    this.namespace = namespace;
  }

  @Nullable
  @Override
  public Object getProperty(String name) {
    ByteSequence key = getKey(name);
    try {
      List<KeyValue> kvs = source.get(key).get().getKvs();
      KeyValue keyValue = CollectionUtils.firstElement(kvs);
      if (keyValue != null) {
        logger.info("found property: {}: key: {} value: {}", name, key, keyValue.getValue());
        return keyValue.getValue().toString();
      }
      return null;
    }
    catch (Exception e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  private ByteSequence getKey(String name) {
    if (namespace != null) {
      return ByteSequence.from(namespace + name, StandardCharsets.UTF_8);
    }
    return ByteSequence.from(name, StandardCharsets.UTF_8);
  }

}
