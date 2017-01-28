package com.afollestad.inquiry;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.Table;

/**
 * @author Aidan Follestad (afollestad)
 */
@Table(name = "children") public class Child {

    public Child() {
        // Default constructor is needed so Inquiry can auto construct instances
    }

    public Child(String name) {
        this.id = 0;
        this.name = name;
        this.parentId = 0;
    }

    @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
    public long id;
    @Column public String name;
    @Column public long parentId;

    public Person parent;
}
