package com.afollestad.inquirysample;

import com.afollestad.inquiry.annotations.Column;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Creator {

    public Creator() {
    }

    public Creator(String name) {
        this();
        this.name = name;
    }

    @Column(primaryKey = true, autoIncrement = true)
    public long _id;
    @Column
    public String name;
    @Column
    public int exerciseId;

    public Exercise exercise;
}