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

package cn.taketoday.rpc;

import java.io.Serializable;

/**
 * @author TODAY 2021/7/4 22:31
 */
public class RpcResponse implements Serializable {
  /** service result */
  private Object result;

  public RpcResponse() { }

  public RpcResponse(Object result) {
    this.result = result;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  public Object getResult() {
    return result;
  }

  public static RpcResponse of(Object result) {
    return new RpcResponse(result);
  }

}
