package com.afollestad.inquirysample.reference;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.afollestad.inquiry.Inquiry;
import com.afollestad.inquiry.callbacks.GetCallback;
import com.afollestad.inquiry.callbacks.RunCallback;
import com.afollestad.inquirysample.R;

import java.util.Arrays;

/**
 * @author Heinrich Reimer (HeinrichReimer)
 */
public class ReferenceActivity extends AppCompatActivity {

    private static final String TAG = "ReferenceActivity";

    private ReferenceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Inquiry.init(this, "people", 3);
        setContentView(R.layout.activity_reference);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        adapter = new ReferenceAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        reload();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing())
            Inquiry.deinit();
    }

    private void insert(){
        SimplePerson waverly = new SimplePerson("Waverly");
        Person aidan = new Person("Aidan", 20, waverly);

        SimplePerson lena = new SimplePerson("Lena");
        Person heinrich = new Person("Heinrich", 18, lena);

        Inquiry.get()
                .insertInto("people", Person.class)
                .values(aidan, heinrich)
                .run(new RunCallback<Long[]>() {
                    @Override
                    public void result(Long[] ids) {
                        Log.d(TAG, "Inserted persons. IDs: " + Arrays.toString(ids));
                    }
                });
    }

    private void reload(){
        Inquiry.get()
                .selectFrom("people", Person.class)
                .all(new GetCallback<Person>() {
                    @Override
                    public void result(@Nullable Person[] persons) {
                        Log.d(TAG, "Loaded persons: " + Arrays.toString(persons));
                        adapter.setPersons(persons);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_references, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_item_load_persons){
            reload();
            return true;
        }
        if(item.getItemId() == R.id.menu_item_insert_persons){
            insert();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
