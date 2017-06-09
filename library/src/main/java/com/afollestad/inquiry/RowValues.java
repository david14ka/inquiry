package com.afollestad.inquiry;

import android.content.ContentValues;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** @author Aidan Follestad (afollestad) */
class RowValues implements Iterable<Map.Entry<String, Object>> {

  private Map<String, Object> values;

  RowValues() {
    this.values = new HashMap<>(0);
  }

  public void put(@NonNull String key, String value) {
    values.put(key, value);
  }

  @Nullable
  @CheckResult
  public String getString(@NonNull String key) {
    return (String) values.get(key);
  }

  public void put(@NonNull String key, Short value) {
    values.put(key, value);
  }

  @Nullable
  @CheckResult
  public Short getShort(@NonNull String key) {
    return (Short) values.get(key);
  }

  public void put(@NonNull String key, Integer value) {
    values.put(key, value);
  }

  @Nullable
  @CheckResult
  public Integer getInt(@NonNull String key) {
    return (Integer) values.get(key);
  }

  public void put(@NonNull String key, Long value) {
    values.put(key, value);
  }

  @Nullable
  @CheckResult
  public Long getLong(@NonNull String key) {
    return (Long) values.get(key);
  }

  public void put(@NonNull String key, Float value) {
    values.put(key, value);
  }

  @Nullable
  @CheckResult
  public Float getFloat(@NonNull String key) {
    return (Float) values.get(key);
  }

  public void put(@NonNull String key, Double value) {
    values.put(key, value);
  }

  @Nullable
  @CheckResult
  public Double getDouble(@NonNull String key) {
    return (Double) values.get(key);
  }

  public void put(@NonNull String key, Boolean value) {
    values.put(key, value);
  }

  @Nullable
  @CheckResult
  public Boolean getBool(@NonNull String key) {
    Object value = values.get(key);
    if (value instanceof Integer) return (Integer) value == 1;
    return (Boolean) value;
  }

  public void put(@NonNull String key, Byte value) {
    values.put(key, value);
  }

  @Nullable
  @CheckResult
  public Byte getByte(@NonNull String key) {
    return (Byte) values.get(key);
  }

  public void put(@NonNull String key, byte[] value) {
    values.put(key, value);
  }

  @Nullable
  @CheckResult
  public byte[] getByteArray(@NonNull String key) {
    return (byte[]) values.get(key);
  }

  @Override
  public Iterator<Map.Entry<String, Object>> iterator() {
    return values.entrySet().iterator();
  }

  @CheckResult
  public int size() {
    return values.size();
  }

  @NonNull
  @CheckResult
  ContentValues toContentValues() {
    ContentValues contentValues = new ContentValues(values.size());
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      if (entry.getValue() == null) contentValues.putNull(entry.getKey());
      else if (entry.getValue() instanceof String)
        contentValues.put(entry.getKey(), (String) entry.getValue());
      else if (entry.getValue() instanceof Short)
        contentValues.put(entry.getKey(), (Short) entry.getValue());
      else if (entry.getValue() instanceof Integer)
        contentValues.put(entry.getKey(), (Integer) entry.getValue());
      else if (entry.getValue() instanceof Long)
        contentValues.put(entry.getKey(), (Long) entry.getValue());
      else if (entry.getValue() instanceof Float)
        contentValues.put(entry.getKey(), (Float) entry.getValue());
      else if (entry.getValue() instanceof Boolean)
        contentValues.put(entry.getKey(), (Boolean) entry.getValue());
      else if (entry.getValue() instanceof Byte)
        contentValues.put(entry.getKey(), (Byte) entry.getValue());
      else if (entry.getValue() instanceof byte[])
        contentValues.put(entry.getKey(), (byte[]) entry.getValue());
      else throw new IllegalStateException("Unknown entry type: " + entry.getValue().getClass());
    }
    return contentValues;
  }

  @Override
  public String toString() {
    return values.toString();
  }
}
