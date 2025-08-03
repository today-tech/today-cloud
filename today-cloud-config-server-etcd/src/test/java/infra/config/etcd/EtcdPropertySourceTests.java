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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/10/6 22:20
 */
@Disabled
class EtcdPropertySourceTests {

  @Test
  void test() throws InterruptedException {
    Client client = Client.builder()
            .endpoints("http://localhost:2379")
            .user(ByteSequence.from("root", StandardCharsets.UTF_8))
            .password(ByteSequence.from("88888888", StandardCharsets.UTF_8))
            .build();

    KV kvClient = client.getKVClient();
    EtcdPropertySource source = new EtcdPropertySource(kvClient);

    Object property = source.getProperty("key");
    System.out.println(property);

    Watch watchClient = client.getWatchClient();
    watchClient.watch(ByteSequence.from("cn.taketoday.blog.BlogApplication", StandardCharsets.UTF_8),
            WatchOption.newBuilder().isPrefix(true).build(), new Watch.Listener() {

              @Override
              public void onNext(WatchResponse response) {
                System.out.println(response);
              }

              @Override
              public void onError(Throwable throwable) {
                throwable.printStackTrace();
              }

              @Override
              public void onCompleted() {
                System.out.println("onCompleted");
              }
            });

    Thread.currentThread().join();
  }

}