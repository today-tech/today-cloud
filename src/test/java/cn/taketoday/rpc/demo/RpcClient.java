/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2021 All Rights Reserved.
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

package cn.taketoday.rpc.demo;

import cn.taketoday.rpc.demo.service.UserService;
import cn.taketoday.rpc.protocol.http.HttpServiceRegistry;

/**
 * @author TODAY 2021/7/3 22:47
 */
public class RpcClient {

  public static void main(String[] args) {
    final HttpServiceRegistry serviceRegistry = HttpServiceRegistry.ofURL("http://localhost:8080/services");
    UserService userService = serviceRegistry.lookup(UserService.class);

    final String ret = userService.hello("today rpc");
    System.out.println(ret);

  }

}
