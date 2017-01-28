package com.afollestad.inquiry;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.Table;

/**
 * @author Aidan Follestad (afollestad)
 */
@Table public class BuilderRow {

    private final long id;
    private final String username;
    private final int age;
    private final boolean online;
    private final float rank;

    private BuilderRow(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.age = builder.age;
        this.online = builder.online;
        this.rank = builder.rank;
    }

    @Column(autoIncrement = true, name = "_id", primaryKey = true)
    public long id() {
        return id;
    }

    @Column public String username() {
        return username;
    }

    @Column public int age() {
        return age;
    }

    @Column public boolean online() {
        return online;
    }

    @Column public float rank() {
        return rank;
    }

    public static class Builder {

        private long id;
        private String username;
        private int age;
        private boolean online;
        private float rank;

        public Builder() {
        }

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder online(boolean online) {
            this.online = online;
            return this;
        }

        public Builder rank(float rank) {
            this.rank = rank;
            return this;
        }

        public BuilderRow build() {
            return new BuilderRow(this);
        }
    }
}
