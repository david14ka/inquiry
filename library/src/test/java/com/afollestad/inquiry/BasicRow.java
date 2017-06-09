package com.afollestad.inquiry;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.Table;

/** @author Aidan Follestad (afollestad) */
@Table
class BasicRow {

  public BasicRow() {}

  BasicRow(String username, int age, boolean online, float rank) {
    this.username = username;
    this.age = age;
    this.online = online;
    this.rank = rank;
  }

  @Column(autoIncrement = true, name = "_id", primaryKey = true)
  long id;

  @Column String username;
  @Column int age;
  @Column boolean online;
  @Column float rank;
}
