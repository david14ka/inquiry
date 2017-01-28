package com.afollestad.inquiry;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class BasicInstrumentTest {

    private static final String INSTANCE_NAME = "test";

    @SuppressWarnings("CheckResult") @Before public void setup() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        Inquiry inq = Inquiry.newInstance(appContext, "test.dn")
                .instanceName(INSTANCE_NAME)
                .build();
        inq.delete(Person.class);
        inq.delete(Child.class);
    }

    @Test public void test_insert_and_query() throws Exception {
        Person[] people = new Person[]{
                new Person("Natalie"),
                new Person("Angela")
        };

        people[0].children.add(new Child("Aidan"));
        people[1].children.add(new Child("Dylan"));
        people[1].children.add(new Child("Elias"));

        Long[] insertedIds = Inquiry.get(INSTANCE_NAME)
                .insert(Person.class)
                .values(people)
                .run();
        assertEquals(insertedIds.length, 2);

        Child[] insertedChildren = Inquiry.get(INSTANCE_NAME)
                .select(Child.class)
                .all();
        assertNotNull("No children loaded from the database!", insertedChildren);
        assertEquals(insertedChildren.length, 3) ;
        assertEquals(insertedChildren[0].name, "Aidan");
        assertEquals(insertedChildren[0].parentId, (long) insertedIds[0]);
        assertEquals(insertedChildren[1].name, "Dylan");
        assertEquals(insertedChildren[1].parentId, (long) insertedIds[1]);
        assertEquals(insertedChildren[2].name, "Elias");
        assertEquals(insertedChildren[2].parentId, (long) insertedIds[1]);

        Person[] queriedPeople = Inquiry.get(INSTANCE_NAME)
                .select(Person.class)
                .all();
        assertNotNull("No parent people loaded from the database!", queriedPeople);
        assertEquals(queriedPeople.length, 2);

        Person firstPerson = queriedPeople[0];
        assertEquals(firstPerson.name, "Natalie");
        assertEquals(firstPerson.children.size(), 1);
        assertEquals(firstPerson.children.get(0).name, "Aidan");

        Person secondPerson = queriedPeople[1];
        assertEquals(secondPerson.name, "Angela");
        assertEquals(secondPerson.children.size(), 2);
        assertEquals(firstPerson.children.get(0).name, "Dylan");
        assertEquals(firstPerson.children.get(0).name, "Elias");
    }

    @After public void cleanup() {
        Inquiry.destroy(INSTANCE_NAME);
    }
}
