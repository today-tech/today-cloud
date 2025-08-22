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

package infra.cloud.serialize.format.buffer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

/**
 * Wraps the difference of access methods to DirectBuffers between Android and others.
 */
class DirectBufferAccess {

  private DirectBufferAccess() {
  }

  enum DirectBufferConstructorType {
    ARGS_LONG_LONG,
    ARGS_LONG_INT_REF,
    ARGS_LONG_INT,
    ARGS_INT_INT,
    ARGS_MB_INT_INT
  }

  // For Java >=9, invokes a jdk.internal.ref.Cleaner
  static Method mInvokeCleaner;

  // TODO We should use MethodHandle for efficiency, but it is not available in JDK6
  static Constructor<?> byteBufferConstructor;
  static Class<?> directByteBufferClass;
  static DirectBufferConstructorType directBufferConstructorType;
  static Method memoryBlockWrapFromJni;

  static {
    try {
      final ByteBuffer direct = ByteBuffer.allocateDirect(1);
      // Find the hidden constructor for DirectByteBuffer
      directByteBufferClass = direct.getClass();
      Constructor<?> directByteBufferConstructor = null;
      DirectBufferConstructorType constructorType = null;
      Method mbWrap = null;
      try {
        // JDK21 DirectByteBuffer(long, long)
        directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(long.class, long.class);
        constructorType = DirectBufferConstructorType.ARGS_LONG_LONG;
      }
      catch (NoSuchMethodException e00) {
        try {
          // TODO We should use MethodHandle for Java7, which can avoid the cost of boxing with JIT optimization
          directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(long.class, int.class, Object.class);
          constructorType = DirectBufferConstructorType.ARGS_LONG_INT_REF;
        }
        catch (NoSuchMethodException e0) {
          try {
            // https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/java/nio/DirectByteBuffer.java
            // DirectByteBuffer(long address, int capacity)
            directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(long.class, int.class);
            constructorType = DirectBufferConstructorType.ARGS_LONG_INT;
          }
          catch (NoSuchMethodException e1) {
            try {
              directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(int.class, int.class);
              constructorType = DirectBufferConstructorType.ARGS_INT_INT;
            }
            catch (NoSuchMethodException e2) {
              Class<?> aClass = Class.forName("java.nio.MemoryBlock");
              mbWrap = aClass.getDeclaredMethod("wrapFromJni", int.class, long.class);
              mbWrap.setAccessible(true);
              directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(aClass, int.class, int.class);
              constructorType = DirectBufferConstructorType.ARGS_MB_INT_INT;
            }
          }
        }
      }

      byteBufferConstructor = directByteBufferConstructor;
      directBufferConstructorType = constructorType;
      memoryBlockWrapFromJni = mbWrap;

      if (byteBufferConstructor == null) {
        throw new RuntimeException("Constructor of DirectByteBuffer is not found");
      }

      try {
        byteBufferConstructor.setAccessible(true);
      }
      catch (RuntimeException e) {
        // This is a Java9+ exception, so we need to detect it without importing it for Java8 support
        if ("java.lang.reflect.InaccessibleObjectException".equals(e.getClass().getName())) {
          byteBufferConstructor = null;
        }
        else {
          throw e;
        }
      }

      setupCleanerJava9(direct);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void setupCleanerJava9(final ByteBuffer direct) {
    Object obj = getInvokeCleanerMethod(direct);
    if (obj instanceof Throwable) {
      throw new RuntimeException((Throwable) obj);
    }
    mInvokeCleaner = (Method) obj;
  }

  /**
   * Checks if we have a usable {@link Unsafe#invokeCleaner}.
   *
   * @param direct a direct buffer
   * @return the method or an error
   */
  private static Object getInvokeCleanerMethod(ByteBuffer direct) {
    try {
      // See https://bugs.openjdk.java.net/browse/JDK-8171377
      Method m = MessageBuffer.unsafe.getClass().getDeclaredMethod(
              "invokeCleaner", ByteBuffer.class);
      m.invoke(MessageBuffer.unsafe, direct);
      return m;
    }
    catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      return e;
    }
  }

  static long getAddress(Buffer buffer) {
    return ((DirectBuffer) buffer).address();
  }

  static ByteBuffer newByteBuffer(long address, int index, int length, ByteBuffer reference) {
    if (byteBufferConstructor == null) {
      throw new IllegalStateException("Can't create a new DirectByteBuffer. In JDK17+, two JVM options needs to be set: " +
              "--add-opens=java.base/java.nio=ALL-UNNAMED and --add-opens=java.base/sun.nio.ch=ALL-UNNAMED");
    }
    try {
      return switch (directBufferConstructorType) {
        case ARGS_LONG_LONG -> (ByteBuffer) byteBufferConstructor.newInstance(address + index, (long) length);
        case ARGS_LONG_INT_REF -> (ByteBuffer) byteBufferConstructor.newInstance(address + index, length, reference);
        case ARGS_LONG_INT -> (ByteBuffer) byteBufferConstructor.newInstance(address + index, length);
        case ARGS_INT_INT -> (ByteBuffer) byteBufferConstructor.newInstance((int) address + index, length);
        case ARGS_MB_INT_INT -> (ByteBuffer) byteBufferConstructor.newInstance(
                memoryBlockWrapFromJni.invoke(null, address + index, length), length, 0);
      };
    }
    catch (Throwable e) {
      // Convert checked exception to unchecked exception
      throw new RuntimeException(e);
    }
  }
}
