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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.taketoday.web.Constant;

import static cn.taketoday.context.Constant.DEFAULT_CHARSET;

/**
 * @author TODAY 2021/7/4 23:15
 */
public abstract class HttpUtils {

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
      throw new HttpRuntimeException("cannot open connection", e);
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
      throw new HttpRuntimeException("cannot open connection", e);
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
      throw new HttpRuntimeException("cannot read response", e);
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
   */
  public static String getResponse(String method, String urlStr, byte[] body) {
    return getResponse(getConnection(method, urlStr, body));
  }

  public static String getResponse(String method, String urlStr, String body) {
    return getResponse(getConnection(method, urlStr, body.getBytes(DEFAULT_CHARSET)));
  }

  /**
   *
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
