package com.afollestad.inquirysample;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.ForeignKey;

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

    @ForeignKey(tableName = "people", foreignColumnName = "exerciseId", inverseFieldName = "exercise")
    public Creator creator;
}