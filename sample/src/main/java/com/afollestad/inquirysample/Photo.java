package com.afollestad.inquirysample;

import android.provider.MediaStore;

import com.afollestad.inquiry.annotations.Column;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Photo {

    public Photo() {
    }

    @Column(name = MediaStore.Images.Media._ID)
    private long id;
    @Column(name = MediaStore.Images.Media.TITLE)
    private String title;
    @Column(name = MediaStore.Images.Media.DATA)
    private String path;
    @Column(name = MediaStore.Images.Media.DATE_MODIFIED)
    private long dateModified;

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public long getDateModified() {
        return dateModified;
    }
}