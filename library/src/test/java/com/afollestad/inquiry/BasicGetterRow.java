package com.afollestad.inquiry;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.Table;

/**
 * @author Aidan Follestad (afollestad)
 */
@Table public class BasicGetterRow {

    private long id;
    private String username;
    private int age;
    private boolean online;
    private float rank;

    public BasicGetterRow() {
    }

    BasicGetterRow(String username, int age, boolean online, float rank) {
        this.username = username;
        this.age = age;
        this.online = online;
        this.rank = rank;
    }

    @Column(autoIncrement = true, name = "_id", primaryKey = true)
    public long id() {
        return id;
    }

    @Column public String username() {
        return username;
    }

    @Column public int age() {
        return age;
    }

    @Column public boolean online() {
        return online;
    }

    @Column public float rank() {
        return rank;
    }
}