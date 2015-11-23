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
    public long id;
    @Column(name = MediaStore.Images.Media.TITLE)
    public String title;
    @Column(name = MediaStore.Images.Media.DATA)
    public String path;
    @Column(name = MediaStore.Images.Media.DATE_MODIFIED)
    public long dateModified;
}