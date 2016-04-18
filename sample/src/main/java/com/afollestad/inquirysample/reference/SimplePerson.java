package com.afollestad.inquirysample.reference;

import com.afollestad.inquiry.annotations.Column;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SimplePerson {

    @Column(autoIncrement = true, name = "_id", primaryKey = true)
    private long id;
    @Column
    private String name;

    public SimplePerson() {
    }

    public SimplePerson(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}