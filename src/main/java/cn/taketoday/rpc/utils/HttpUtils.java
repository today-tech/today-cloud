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

import cn.taketoday.web.Constant;

import static cn.taketoday.context.Constant.DEFAULT_CHARSET;

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

  /**
   * get a connection with request body
   *
   * @param method
   *         request method
   * @param urlStr
   *         url
   * @param body
   *         request body
   */
  public static HttpURLConnection getConnection(String method, String urlStr, byte[] body) {
    return getConnection(method, urlStr, Constant.CONTENT_TYPE_JSON, body);
  }

  /**
   * get a connection with request body
   *
   * @param method
   *         request method
   * @param urlStr
   *         url
   * @param body
   *         request body
   */
  public static HttpURLConnection getConnection(
          String method, String urlStr, String contentType, byte[] body) {
    HttpURLConnection connection = getConnection(method, urlStr);
    if (contentType != null) {
      connection.setRequestProperty(Constant.CONTENT_TYPE, contentType);
    }
    if (body != null) {
      try {
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(body);
      }
      catch (IOException e) {
        throw new HttpRuntimeException("cannot get output-stream", e);
      }
    }
    return connection;
  }

  /**
   * get a connection
   *
   * @param method
   *         request method
   * @param urlStr
   *         url
   */
  public static HttpURLConnection getConnection(String method, String urlStr) {
    try {
      URL url = new URL(urlStr);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(10000);
      if (!"GET".equals(method)) {
        connection.setDoInput(true);// 允许输入
      }
      connection.setDoOutput(true);// 允许输出
      connection.setUseCaches(false); // 不允许使用缓存
      connection.setRequestMethod(method);
      return connection;
    }
    catch (IOException e) {
      throw new HttpRuntimeException();
    }
  }

  /**
   * @param urlStr
   */
  public static HttpURLConnection getConnection(String urlStr) {
    try {
      URL url = new URL(urlStr);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(3000);
      connection.setDoOutput(true);// 允许输出
      connection.setUseCaches(true); // 不允许使用缓存
      return connection;
    }
    catch (IOException e) {
      throw new HttpRuntimeException();
    }
  }

  public static String getResponse(HttpURLConnection conn) {

    try (BufferedReader reader
            = new BufferedReader(new InputStreamReader(conn.getInputStream(), DEFAULT_CHARSET))) {
      String line;
      final StringBuilder response = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      conn.disconnect();
      return response.toString();
    }
    catch (IOException e) {
      throw new HttpRuntimeException();
    }
  }

  /**
   * get response string
   *
   * @param method
   *         request method
   * @param urlStr
   *         url
   */
  public static String getResponse(String method, String urlStr) {
    return getResponse(getConnection(method, urlStr));
  }

  /**
   * @param method
   *         request method
   * @param urlStr
   *         url
   * @param body
   *
   * @return
   *
   * @throws IOException
   */
  public static String getResponse(String method, String urlStr, byte[] body) {
    return getResponse(getConnection(method, urlStr, body));
  }

  public static String getResponse(String method, String urlStr, String body) {
    return getResponse(getConnection(method, urlStr, body.getBytes(DEFAULT_CHARSET)));
  }

  /**
   * @param method
   * @param urlStr
   * @param body
   * @param targetClass
   *
   * @return
   *
   * @throws IOException
   */
  public static <T> T getResponse(String method, String urlStr, byte[] body, Class<T> targetClass) {
    return ObjectMapperUtils.fromJSON(getResponse(method, urlStr, body), targetClass);
  }

  // GET
  // ---------------------------------

  public static String get(String urlStr) {
    return getResponse(getConnection("GET", urlStr));
  }

  // POST
  // ---------------------------------

  public static String post(String urlStr) {
    return getResponse(getConnection("POST", urlStr, null, null));
  }

  public static String post(String urlStr, String params) {
    return getResponse(getConnection("POST", urlStr, null, params.getBytes(DEFAULT_CHARSET)));
  }

  public static <T> T post(String urlStr, String params, Class<T> targetClass) {
    return ObjectMapperUtils.fromJSON(post(urlStr, params), targetClass);
  }

  public static String postJson(String urlStr, byte[] body) {
    return getResponse(getConnection("POST", urlStr, body));
  }

  public static String postJson(String urlStr, String body) {
    return getResponse(getConnection("POST", urlStr, body.getBytes(DEFAULT_CHARSET)));
  }

  public static <T> T postJson(String urlStr, byte[] body, Class<T> targetClass) {
    return ObjectMapperUtils.fromJSON(postJson(urlStr, body), targetClass);
  }

  public static <T> T postJson(String urlStr, String body, Class<T> targetClass) {
    return ObjectMapperUtils.fromJSON(postJson(urlStr, body), targetClass);
  }
  // PUT
  // ---------------------------------

  public static String put(String urlStr) {
    return getResponse(getConnection("PUT", urlStr));
  }

  public static String put(String urlStr, byte[] body) {
    return getResponse(getConnection("PUT", urlStr, body));
  }

  public static String put(String urlStr, String body) {
    return getResponse(getConnection("PUT", urlStr, body.getBytes(DEFAULT_CHARSET)));
  }

  public static <T> T put(String urlStr, byte[] body, Class<T> targetClass) {
    return ObjectMapperUtils.fromJSON(getResponse("PUT", urlStr, body), targetClass);
  }

  // DELETE
  // ---------------------------------

  public static String delete(String urlStr) {
    return getResponse(getConnection("DELETE", urlStr));
  }

  public static String delete(String urlStr, byte[] body) {
    return getResponse(getConnection("DELETE", urlStr, body));
  }

  public static String delete(String urlStr, String body) {
    return getResponse(getConnection("DELETE", urlStr, body.getBytes(DEFAULT_CHARSET)));
  }

  public static <T> T delete(String urlStr, byte[] body, Class<T> targetClass) {
    return ObjectMapperUtils.fromJSON(getResponse("DELETE", urlStr, body), targetClass);
  }
}
