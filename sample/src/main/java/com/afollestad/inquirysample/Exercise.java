package com.afollestad.inquirysample;

import com.afollestad.inquiry.annotations.Column;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Exercise {

    public Exercise() {
    }

    public Exercise(String name, String instructions) {
        this();
        this.name = name;
        this.instructions = instructions;
    }

    @Column(primaryKey = true, autoIncrement = true)
    public long _id;
    @Column
    public long skillId;
    @Column
    public String name;
    @Column
    public String instructions;
    public Skill skill;
}