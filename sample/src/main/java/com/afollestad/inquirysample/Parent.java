package com.afollestad.inquirysample;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.ForeignKey;
import com.afollestad.inquiry.lazyloading.LazyLoaderList;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Parent {

    public Parent() {
        children = new LazyLoaderList<>();
    }

    public Parent(String name) {
        this();
        this.name = name;
    }

    @Column(name = "_id", primaryKey = true, autoIncrement = true, notNull = true)
    public long id;
    @Column
    public String name;
    @ForeignKey(tableName = "children", foreignColumnName = "parentId", inverseFieldName = "parent")
    public LazyLoaderList<Child> children;

    @Override
    public String toString() {
        return id + ", " + name;
    }
}