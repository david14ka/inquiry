package com.afollestad.inquiry;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.Table;
import com.google.auto.value.AutoValue;

/**
 * @author Aidan Follestad (afollestad)
 */
@Table
@AutoValue
public abstract class AutoValueRow {

    public static Builder create() {
        return new AutoValue_AutoValueRow.Builder();
    }

    public AutoValueRow withId(long id) {
        return new AutoValue_AutoValueRow.Builder(this).id(id).build();
    }

    @Column(autoIncrement = true, name = "_id", primaryKey = true)
    public abstract long id();

    @Column public abstract String username();

    @Column public abstract int age();

    @Column public abstract boolean online();

    @Column public abstract float rank();

    @AutoValue.Builder static abstract class Builder {

        public abstract Builder id(long id);

        public abstract Builder username(String username);

        public abstract Builder age(int age);

        public abstract Builder online(boolean online);

        public abstract Builder rank(float rank);

        public abstract AutoValueRow build();
    }
}