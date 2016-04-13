package com.afollestad.inquirysample.reference;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.Reference;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Person {

    @Column(autoIncrement = true, name = "_id", primaryKey = true)
    private long id;
    @Column(name = "name")
    private String name;
    @Column(name = "age")
    private int age;
    @Reference(columnName = "spouse", tableName = "spouses")
    private SimplePerson spouse;

    public Person() {
    }

    public Person(String name, int age, SimplePerson spouse) {
        this.name = name;
        this.age = age;
        this.spouse = spouse;
    }

    @Override
    public String toString() {
        return name + " (" + age + ", " + spouse + ")";
    }
}