package com.afollestad.inquiry;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.ForeignKey;
import com.afollestad.inquiry.annotations.Table;
import com.afollestad.inquiry.lazyloading.LazyLoaderList;

/**
 * @author Aidan Follestad (afollestad)
 */
@Table public class Person {

    public Person() {
        // Default constructor is needed so Inquiry can auto construct instances
    }

    public Person(String name, int age) {
        this.id = 0;
        this.name = name;
        this.age = age;
        this.children = new LazyLoaderList<>(0);
    }

    @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
    public long id;
    @Column public String name;
    @Column public int age;

    // inverseFieldName is optional, here it refers to the "parent" field in the Child class which gets set to a reference to this Person
    @ForeignKey(tableName = "children", foreignColumnName = "parentId", inverseFieldName = "parent")
    public LazyLoaderList<Child> children;
}
