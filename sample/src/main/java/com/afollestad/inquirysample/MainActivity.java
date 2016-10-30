package com.afollestad.inquirysample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.inquiry.Inquiry;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Inquiry.newInstance(this, null)
                .build();
        query();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing())
            Inquiry.destroy(this);
    }

    private void query() {
        Inquiry.get(this)
                .selectFrom("rows", Row.class)
                .all(result -> {
                    if (result == null || result.length == 0) {
                        insert();
                    } else {
                        Log.d("MainActivity", "Got " + result.length + " rows.");
                    }
                });
    }

    private void insert() {
        Inquiry.get(this)
                .insertInto("rows", Row.class)
                .values(new Row("Aidan"), new Row("Waverly"))
                .run(changed -> query());
    }
}