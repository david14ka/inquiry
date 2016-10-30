package com.afollestad.inquirysample;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.ForeignChildren;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Skill {

    public Skill() {
        exercises = new ArrayList<>();
    }

    public Skill(String name, int level) {
        this();
        this.name = name;
        this.level = level;
    }

    @Column(primaryKey = true, autoIncrement = true)
    public long _id;
    @Column
    public long skillAreaId;
    @Column
    public String name;
    @Column
    public int level;

    public SkillArea skillArea;
    @ForeignChildren(tableName = "exercises", foreignColumnName = "skillId", inverseFieldName = "skill")
    public List<Exercise> exercises;
}