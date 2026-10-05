/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.cloud.service;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import infra.util.Assert;

/**
 * Explicit cross-process metadata, separate from local invocation attributes.
 * Entries preserve order and duplicate names. Binary values are defensively copied.
 * Mutable instances are intended for client-side construction, not concurrent use;
 * {@link #snapshot()} creates an immutable value suitable for a decoded request.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2026/10/5
 */
public final class Metadata {

  public static final Metadata EMPTY = new Metadata(List.of(), false);

  private final List<Entry> entries;

  private final boolean mutable;

  public Metadata() {
    this(new ArrayList<>(), true);
  }

  private Metadata(List<Entry> entries, boolean mutable) {
    this.entries = entries;
    this.mutable = mutable;
  }

  public static Metadata empty() {
    return EMPTY;
  }

  /**
   * Append a UTF-8 value without replacing existing entries.
   */
  public Metadata add(String name, String value) {
    Assert.notNull(value, "value is required");
    return append(new Entry(name, value.getBytes(StandardCharsets.UTF_8), false));
  }

  /**
   * Append binary data; the supplied array is copied.
   */
  public Metadata addBinary(String name, byte[] value) {
    return append(new Entry(name, value, true));
  }

  private Metadata append(Entry entry) {
    Assert.state(mutable, "Metadata snapshot is immutable");
    entries.add(entry);
    return this;
  }

  /**
   * Return the first text value with the given name, or null.
   */
  public @Nullable String get(String name) {
    for (Entry entry : entries) {
      if (!entry.binary && entry.name.equals(name)) {
        return new String(entry.value, StandardCharsets.UTF_8);
      }
    }
    return null;
  }

  /** Return a copy of the first binary value with the given name, or null. */
  public byte @Nullable [] getBinary(String name) {
    for (Entry entry : entries) {
      if (entry.binary && entry.name.equals(name)) {
        return entry.value();
      }
    }
    return null;
  }

  public List<Entry> entries() {
    return List.copyOf(entries);
  }

  public boolean isEmpty() {
    return entries.isEmpty();
  }

  public Metadata snapshot() {
    return entries.isEmpty() ? EMPTY : mutable
            ? new Metadata(List.copyOf(entries), false) : this;
  }

  /**
   * Immutable entry; binary and text values have distinct wire tags.
   */
  public static final class Entry {

    private final String name;

    private final byte[] value;

    private final boolean binary;

    private Entry(String name, byte[] value, boolean binary) {
      Assert.hasText(name, "metadata name is required");
      Assert.notNull(value, "value is required");
      this.name = name;
      this.value = value.clone();
      this.binary = binary;
    }

    public String name() {
      return name;
    }

    public byte[] value() {
      return value.clone();
    }

    public boolean binary() {
      return binary;
    }

    // Codec-only access; callers outside this package receive a defensive copy.
    byte[] rawValue() {
      return value;
    }
  }
}
