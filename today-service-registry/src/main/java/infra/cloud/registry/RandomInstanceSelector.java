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

package infra.cloud.registry;

import java.util.List;
import java.util.Random;

import infra.cloud.client.ServiceInstance;
import infra.lang.Assert;

/**
 * @author TODAY 2021/7/9 23:20
 */
public class RandomInstanceSelector implements InstanceSelector {
  private final Random random;

  public RandomInstanceSelector() {
    this(new Random());
  }

  public RandomInstanceSelector(Random random) {
    Assert.notNull(random, "Random is required");
    this.random = random;
  }

  @Override
  public ServiceInstance select(List<ServiceInstance> instances) {
    final int size = instances.size();
    final int idx = random.nextInt(size);
    return instances.get(idx);
  }

}
