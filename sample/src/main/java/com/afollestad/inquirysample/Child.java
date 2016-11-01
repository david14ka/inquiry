package com.afollestad.inquirysample;

import com.afollestad.inquiry.annotations.Column;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Child {

    public Child() {
    }

    public Child(String name) {
        this.name = name;
    }

    @Column(name = "_id", primaryKey = true, autoIncrement = true, notNull = true)
    public long id;
    @Column
    public String name;
    @Column
    public long parentId;

    public Parent parent;

    @Override
    public String toString() {
        return id + ", " + name + ", child of " + parent.name;
    }
}