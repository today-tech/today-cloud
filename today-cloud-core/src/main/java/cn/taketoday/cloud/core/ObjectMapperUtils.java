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

package cn.taketoday.cloud.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author TODAY 2021/7/4 00:11
 */
public class ObjectMapperUtils {

  private static ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
  }

  // JSON

  public static ObjectMapper getSharedMapper() {
    return objectMapper;
  }

  public static void setSharedMapper(ObjectMapper objectMapper) {
    ObjectMapperUtils.objectMapper = objectMapper;
  }

  /**
   * javaBean、列表数组转换为json字符串
   */
  public static String toJSON(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static <T> T fromJSON(InputStream in, Class<T> clazz) {
    try {
      return objectMapper.readValue(in, clazz);
    }
    catch (IOException io) {
      throw new IllegalStateException(io);
    }
  }

  public static <T> T fromJSON(String jsonString, Class<T> clazz) {
    try {
      return objectMapper.readValue(jsonString, clazz);
    }
    catch (IOException io) {
      throw new IllegalStateException(io);
    }
  }

  public static <T> T fromJSON(String jsonString, TypeReference<T> reference) {
    try {
      return objectMapper.readValue(jsonString, reference);
    }
    catch (IOException io) {
      throw new IllegalStateException(io);
    }
  }

  /**
   * writeValue
   */
  public static void writeValue(OutputStream out, Object object) throws IOException {
    objectMapper.writeValue(out, object);
  }

  /**
   * json字符串转换为map
   */
  public static Map<String, Object> toMap(String jsonString) throws IOException {
    return objectMapper.readValue(jsonString, Map.class);
  }

}
