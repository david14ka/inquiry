package com.afollestad.inquiry;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.Table;

/**
 * @author Aidan Follestad (afollestad)
 */
@Table public class SetterRow {

    private long id;
    private String username;
    private int age;
    private boolean online;
    private float rank;

    public SetterRow() {
    }

    SetterRow(String username, int age, boolean online, float rank) {
        this.username = username;
        this.age = age;
        this.online = online;
        this.rank = rank;
    }

    @Column(autoIncrement = true, name = "_id", primaryKey = true)
    public long id() {
        return id;
    }

    public void id(long id) {
        this.id = id;
    }

    @Column public String username() {
        return username;
    }

    public void username(String username) {
        this.username = username;
    }

    @Column public int age() {
        return age;
    }

    public void age(int age) {
        this.age = age;
    }

    @Column public boolean online() {
        return online;
    }

    public void online(boolean online) {
        this.online = online;
    }

    @Column public float rank() {
        return rank;
    }

    public void rank(float rank) {
        this.rank = rank;
    }
}