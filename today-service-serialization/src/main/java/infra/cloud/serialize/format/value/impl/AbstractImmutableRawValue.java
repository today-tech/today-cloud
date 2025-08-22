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

package infra.cloud.serialize.format.value.impl;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import infra.cloud.serialize.format.MessagePack;
import infra.cloud.serialize.format.MessageStringCodingException;
import infra.cloud.serialize.format.value.ImmutableRawValue;

public abstract class AbstractImmutableRawValue
        extends AbstractImmutableValue
        implements ImmutableRawValue {
  protected final byte[] data;
  private volatile String decodedStringCache;
  private volatile CharacterCodingException codingException;

  public AbstractImmutableRawValue(byte[] data) {
    this.data = data;
  }

  public AbstractImmutableRawValue(String string) {
    this.decodedStringCache = string;
    this.data = string.getBytes(MessagePack.UTF8);  // TODO
  }

  @Override
  public ImmutableRawValue asRawValue() {
    return this;
  }

  @Override
  public byte[] asByteArray() {
    return Arrays.copyOf(data, data.length);
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return ByteBuffer.wrap(data).asReadOnlyBuffer();
  }

  @Override
  public String asString() {
    if (decodedStringCache == null) {
      decodeString();
    }
    if (codingException != null) {
      throw new MessageStringCodingException(codingException);
    }
    else {
      return decodedStringCache;
    }
  }

  @Override
  public String toJson() {
    StringBuilder sb = new StringBuilder();
    appendJsonString(sb, toString());
    return sb.toString();
  }

  private void decodeString() {
    synchronized(data) {
      if (decodedStringCache != null) {
        return;
      }
      try {
        CharsetDecoder reportDecoder = MessagePack.UTF8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        this.decodedStringCache = reportDecoder.decode(asByteBuffer()).toString();
      }
      catch (CharacterCodingException ex) {
        try {
          CharsetDecoder replaceDecoder = MessagePack.UTF8.newDecoder()
                  .onMalformedInput(CodingErrorAction.REPLACE)
                  .onUnmappableCharacter(CodingErrorAction.REPLACE);
          this.decodedStringCache = replaceDecoder.decode(asByteBuffer()).toString();
        }
        catch (CharacterCodingException neverThrown) {
          throw new MessageStringCodingException(neverThrown);
        }
        this.codingException = ex;
      }
    }
  }

  @Override
  public String toString() {
    if (decodedStringCache == null) {
      decodeString();
    }
    return decodedStringCache;
  }

  static void appendJsonString(StringBuilder sb, String string) {
    sb.append("\"");
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      if (ch < 0x20) {
        switch (ch) {
          case '\n':
            sb.append("\\n");
            break;
          case '\r':
            sb.append("\\r");
            break;
          case '\t':
            sb.append("\\t");
            break;
          case '\f':
            sb.append("\\f");
            break;
          case '\b':
            sb.append("\\b");
            break;
          default:
            // control chars
            escapeChar(sb, ch);
            break;
        }
      }
      else if (ch <= 0x7f) {
        switch (ch) {
          case '\\':
            sb.append("\\\\");
            break;
          case '"':
            sb.append("\\\"");
            break;
          default:
            sb.append(ch);
            break;
        }
      }
      else if (ch >= 0xd800 && ch <= 0xdfff) {
        // surrogates
        escapeChar(sb, ch);
      }
      else {
        sb.append(ch);
      }
    }
    sb.append("\"");
  }

  private static final char[] HEX_TABLE = "0123456789ABCDEF".toCharArray();

  private static void escapeChar(StringBuilder sb, int ch) {
    sb.append("\\u");
    sb.append(HEX_TABLE[(ch >> 12) & 0x0f]);
    sb.append(HEX_TABLE[(ch >> 8) & 0x0f]);
    sb.append(HEX_TABLE[(ch >> 4) & 0x0f]);
    sb.append(HEX_TABLE[ch & 0x0f]);
  }
}
