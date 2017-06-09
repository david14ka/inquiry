package com.afollestad.inquiry;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** @author Aidan Follestad (afollestad) */
class DataType {

  @IntDef({INTEGER, REAL, TEXT, BLOB, UNKNOWN})
  @Retention(RetentionPolicy.SOURCE)
  @interface TypeDef {}

  /**
   * The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude
   * of the value.
   *
   * <p>Translates to short, int, or long in Java (based on what was stored in the column).
   */
  static final int INTEGER = 1;
  /**
   * The value is a floating point value, stored as an 8-byte IEEE floating point number.
   *
   * <p>Translates to a float or double in Java.
   */
  static final int REAL = 2;
  /**
   * The value is a text string, stored using the database encoding (UTF-8, UTF-16BE or UTF-16LE).
   *
   * <p>Translates to a String in Java.
   */
  static final int TEXT = 3;
  /**
   * The value is a blob of data, stored exactly as it was input.
   *
   * <p>Translates to byte[] in Java.
   */
  static final int BLOB = 4;

  static final int UNKNOWN = -1;
}
