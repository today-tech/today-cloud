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

package infra.cloud.serialize.format.value;

/**
 * Representation of MessagePack's String type.
 *
 * MessagePack's String type can represent a UTF-8 string at most 2<sup>64</sup>-1 bytes.
 *
 * Note that the value could include invalid byte sequences. {@code asString()} method throws {@code MessageTypeStringCodingException} if the value includes invalid byte sequence. {@code toJson()} method replaces an invalid byte sequence with <code>U+FFFD replacement character</code>.
 *
 * @see RawValue
 */
public interface StringValue
        extends RawValue {
}
