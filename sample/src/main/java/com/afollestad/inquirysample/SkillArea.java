package com.afollestad.inquirysample;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.ForeignKey;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public class SkillArea {

    public SkillArea() {
        skills = new ArrayList<>();
    }

    public SkillArea(String name) {
        this();
        this.name = name;
    }

    @Column(primaryKey = true, autoIncrement = true)
    public long _id;
    @Column
    public String name;

    @ForeignKey(tableName = "skills", foreignColumnName = "skillAreaId", inverseFieldName = "skillArea")
    public List<Skill> skills;
}