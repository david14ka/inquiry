package com.afollestad.inquiry.callbacks;

/** @author Aidan Follestad (afollestad) */
public interface RunCallback<RunReturn> {

  void result(RunReturn changed);
}
