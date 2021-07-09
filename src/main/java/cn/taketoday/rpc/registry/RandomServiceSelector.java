/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.rpc.registry;

import java.util.List;
import java.util.Random;

import cn.taketoday.context.utils.Assert;

/**
 * @author TODAY 2021/7/9 23:20
 */
public class RandomServiceSelector implements ServiceSelector {
  private final Random random;

  public RandomServiceSelector() {
    this(new Random());
  }

  public RandomServiceSelector(Random random) {
    Assert.notNull(random, "Random must not be null");
    this.random = random;
  }

  @Override
  public ServiceDefinition select(List<ServiceDefinition> definitions) {
    final int size = definitions.size();
    final int idx = random.nextInt(size);
    return definitions.get(idx);
  }

}
