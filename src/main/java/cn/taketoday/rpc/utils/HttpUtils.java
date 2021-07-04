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

package cn.taketoday.rpc.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author TODAY 2021/7/4 23:15
 */
public class HttpUtils {

  public static String doGet(String httpUrl) {
    HttpURLConnection connection = null;

    try {
      // 创建远程url连接对象
      URL url = new URL(httpUrl);
      // 通过远程url连接对象打开一个连接，强转成httpURLConnection类
      connection = (HttpURLConnection) url.openConnection();
      // 设置连接方式：get
      connection.setRequestMethod("GET");
      // 设置连接主机服务器的超时时间：15000毫秒
      connection.setConnectTimeout(15000);
      // 设置读取远程返回的数据时间：60000毫秒
      connection.setReadTimeout(60000);
      // 发送请求
      connection.connect();
      String result = null;
      // 通过connection连接，获取输入流
      if (connection.getResponseCode() == 200) {
        try (InputStream is = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
          // 存放数据
          StringBuilder sbf = new StringBuilder();
          String temp;
          while ((temp = br.readLine()) != null) {
            sbf.append(temp);
            sbf.append("\r\n");
          }
          result = sbf.toString();
        }
      }
      return result;
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    finally {
      if (connection != null)
        connection.disconnect();// 关闭远程连接
    }
  }

  public static String doPost(String httpUrl, String param) {
    HttpURLConnection connection = null;
    String result = null;
    try {
      URL url = new URL(httpUrl);
      // 通过远程url连接对象打开连接
      connection = (HttpURLConnection) url.openConnection();
      // 设置连接请求方式
      connection.setRequestMethod("POST");
      // 设置连接主机服务器超时时间：15000毫秒
      connection.setConnectTimeout(15000);
      // 设置读取主机服务器返回数据超时时间：60000毫秒
      connection.setReadTimeout(60000);
      // 默认值为：false，当向远程服务器传送数据/写数据时，需要设置为true
      connection.setDoOutput(true);
      // 默认值为：true，当前向远程服务读取数据时，设置为true，该参数可有可无
      connection.setDoInput(true);
      // 设置传入参数的格式:请求参数应该是 name1=value1&name2=value2 的形式。
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      // 设置鉴权信息：Authorization: Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0
      // 通过连接对象获取一个输出流
      OutputStream os = connection.getOutputStream();
      // 通过输出流对象将参数写出去/传输出去,它是通过字节数组写出的
      os.write(param.getBytes());
      // 通过连接对象获取一个输入流，向远程读取
      if (connection.getResponseCode() == 200) {
        try (InputStream is = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

          StringBuilder sbf = new StringBuilder();
          String temp;
          // 循环遍历一行一行读取数据
          while ((temp = br.readLine()) != null) {
            sbf.append(temp);
            sbf.append("\r\n");
          }
          result = sbf.toString();
        }
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    finally {
      if (connection != null)
        connection.disconnect();
    }
    return result;
  }

}
