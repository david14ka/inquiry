package com.afollestad.inquirysample;

import com.afollestad.inquiry.annotations.Column;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Row {

    public Row() {
    }

    public Row(String name) {
        this.name = name;
    }

    @Column(name = "_id", primaryKey = true, autoIncrement = true, notNull = true)
    public long id;
    @Column
    public String name;

    @Override
    public String toString() {
        return id + ", " + name;
    }
}