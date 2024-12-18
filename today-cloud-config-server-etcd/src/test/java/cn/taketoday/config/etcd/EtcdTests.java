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

package cn.taketoday.config.etcd;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.util.CollectionUtils;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.GetResponse;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/10/6 20:40
 */
class EtcdTests {

  @Test
  void test() throws Exception {
    Client client = Client.builder()
            .endpoints("http://localhost:2379")
            .user(ByteSequence.from("root", StandardCharsets.UTF_8))
            .password(ByteSequence.from("88888888", StandardCharsets.UTF_8))
            .build();

    KV kvClient = client.getKVClient();

    ByteSequence key = ByteSequence.from("key", StandardCharsets.UTF_8);
    kvClient.put(key, ByteSequence.from("value", StandardCharsets.UTF_8));

    GetResponse response = kvClient.get(key).get();

    System.out.println(CollectionUtils.firstElement(response.getKvs()).getValue());

  }

}
