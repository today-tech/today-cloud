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
 * Representation of MessagePack's Extension type.
 *
 * MessagePack's Extension type can represent represents a tuple of type information and a byte array where type information is an
 * integer whose meaning is defined by applications.
 *
 * As the type information, applications can use 0 to 127 as the application-specific types. -1 to -128 is reserved for MessagePack's future extension.
 */
public interface ExtensionValue
        extends Value {
  byte getType();

  byte[] getData();
}
