package com.afollestad.inquiry;

/** @author Aidan Follestad (afollestad) */
public interface AnyPredicate<T> {

  boolean match(T it);
}
