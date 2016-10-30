package com.afollestad.inquirysample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.inquiry.Inquiry;
import com.afollestad.inquiry.callbacks.GetCallback;
import com.afollestad.inquiry.callbacks.RunCallback;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Inquiry iq = Inquiry.newInstance(this, "test2").build();

        if (!iq.selectFrom("skillAreas", SkillArea.class).any()) {
            SkillArea area1 = new SkillArea("Foundation");
            Skill skill1 = new Skill("Basics", 1);
            Exercise exercise1 = new Exercise("Exercise 1", "The fundamentals.");
            Exercise exercise2 = new Exercise("Exercise 2", "How are you?");
            skill1.exercises.add(exercise1);
            skill1.exercises.add(exercise2);
            area1.skills.add(skill1);
            Skill skill2 = new Skill("Advanced", 2);
            Exercise exercise3 = new Exercise("Taking it to the next level", "Hey");
            Exercise exercise4 = new Exercise("Finishing up", "Hello");
            skill2.exercises.add(exercise3);
            skill2.exercises.add(exercise4);
            area1.skills.add(skill2);

            iq.insertInto("skillAreas", SkillArea.class)
                    .values(area1)
                    .run(new RunCallback<Long[]>() {
                        @Override
                        public void result(Long[] changed) {
                            Log.v("Done", "Done");
                        }
                    });
        } else {
            iq.selectFrom("exercises", Exercise.class)
                    .all(new GetCallback<Exercise>() {
                        @Override
                        public void result(@Nullable Exercise[] result) {
                            Log.v("Done", "Done");

                            Inquiry.get(MainActivity.this)
                                    .selectFrom("skillAreas", SkillArea.class)
                                    .all(new GetCallback<SkillArea>() {
                                        @Override
                                        public void result(@Nullable SkillArea[] result) {
                                            Log.v("Done", "Done");
                                        }
                                    });
                        }
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing())
            Inquiry.destroy(this);
    }
}