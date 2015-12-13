package com.afollestad.inquirysample;

import android.provider.MediaStore;

import com.afollestad.inquiry.annotations.Column;

public class Photo extends Media {
    /* Subclass demo. Inquiry finds all @Column annotations declared in superclasses */

    @Column(name = MediaStore.Images.Media.DATE_TAKEN)
    private long dateTaken;
    @Column(name = MediaStore.Images.Media.WIDTH)
    private int width;
    @Column(name = MediaStore.Images.Media.HEIGHT)
    private int height;
    @Column(name = MediaStore.Images.Media.ORIENTATION)
    private int orientation;

    public long getDateTaken() {
        return dateTaken;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getOrientation() {
        return orientation;
    }
}
