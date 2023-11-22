/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.cloud.registry;

import java.util.List;
import java.util.Random;

import cn.taketoday.cloud.ServiceInstance;
import cn.taketoday.lang.Assert;

/**
 * @author TODAY 2021/7/9 23:20
 */
public class RandomServiceSelector implements ServiceSelector {
  private final Random random;

  public RandomServiceSelector() {
    this(new Random());
  }

  public RandomServiceSelector(Random random) {
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
