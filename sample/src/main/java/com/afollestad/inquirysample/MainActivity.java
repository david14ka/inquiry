package com.afollestad.inquirysample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.inquiry.Inquiry;
import com.facebook.stetho.Stetho;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Stetho.initializeWithDefaults(this);

        // :memory uses in-memory DB which only persists until the app closes. No file saving.
        Inquiry.newInstance(this, "forStetho")
                .build();
        Inquiry.get(this).dropTable(Parent.class);
        query();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing())
            Inquiry.destroy(this);
    }

    private void query() {
        Inquiry.get(MainActivity.this)
                .select(Parent.class)
                .all(result -> {
                    if (result == null || result.length == 0) {
                        insert();
                    } else {
                        Log.d("MainActivity", "Got " + result.length + " rows.");
                        Log.d("MainActivity", "Parent 1 has " + result[0].children.size() + " children."); // Triggers lazy loading
                    }
                });
    }

    private void insert() {
        Parent parent1 = new Parent("Natalie");
        parent1.children.add(new Child("Aidan"));
        parent1.children.add(new Child("Olivia"));
        parent1.children.add(new Child("Elijah"));
        parent1.children.add(new Child("Seven"));
        parent1.children.add(new Child("Rain"));

        Parent parent2 = new Parent("Angela");
        parent2.children.add(new Child("Dylan"));
        parent2.children.add(new Child("Elias"));

        Inquiry.get(this)
                .insert(Parent.class)
                .values(parent1, parent2)
                .run(changed -> query());
    }
}