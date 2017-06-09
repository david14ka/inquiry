package com.afollestad.inquiry;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class InquiryTests {

  private static final String INSTANCE_NAME = "test";

  @SuppressWarnings("CheckResult")
  @Before
  public void setup() {
    Context appContext = InstrumentationRegistry.getTargetContext();
    Inquiry inq =
        Inquiry.newInstance(appContext, "instrument_test").instanceName(INSTANCE_NAME).build();
    inq.delete(Person.class).run();
    inq.delete(Child.class).run();
  }

  @Test
  public void test_insert_and_query() throws Exception {
    Person[] people =
        new Person[] {
          new Person("Natalie", 43),
          new Person("Angela", 41),
          new Person("Jeff", 42),
          new Person("Aidan", 21),
          new Person("Waverly", 19),
          new Person("Jane", 70)
        };
    Long[] insertedIds = Inquiry.get(INSTANCE_NAME).insert(Person.class).values(people).run();
    assertEquals(insertedIds.length, 6);

    Person[] query1 =
        Inquiry.get(INSTANCE_NAME)
            .select(Person.class)
            .where("name = ? OR name = ?", "Aidan", "Waverly")
            .sort("name")
            .all();
    assertNotNull(query1);
    assertEquals(query1.length, 2);
    assertEquals(query1[0].name, "Aidan");
    assertEquals(query1[1].name, "Waverly");

    Person[] query2 =
        Inquiry.get(INSTANCE_NAME).select(Person.class).whereIn("age", 70, 42).sort("name").all();
    assertNotNull(query2);
    assertEquals(query2.length, 2);
    assertEquals(query2[0].name, "Jane");
    assertEquals(query2[1].name, "Jeff");

    Person[] query3 =
        Inquiry.get(INSTANCE_NAME)
            .select(Person.class)
            .where("age >= 19")
            .where("age <= 42")
            .sort("name")
            .all();
    assertNotNull(query3);
    assertEquals(query3.length, 4);
    assertEquals(query3[0].name, "Aidan");
    assertEquals(query3[1].name, "Angela");
    assertEquals(query3[2].name, "Jeff");
    assertEquals(query3[3].name, "Waverly");

    Person[] query4 =
        Inquiry.get(INSTANCE_NAME)
            .select(Person.class)
            .where("age = 19")
            .orWhere("name = ?", "Aidan")
            .sort("name")
            .all();
    assertNotNull(query4);
    assertEquals(query4.length, 2);
    assertEquals(query4[0].name, "Aidan");
    assertEquals(query4[1].name, "Waverly");

    Person[] query5 =
        Inquiry.get(INSTANCE_NAME)
            .select(Person.class)
            .where("name LIKE ?", "%dan")
            .sort("name")
            .all();
    assertNotNull(query5);
    assertEquals(query5.length, 1);
    assertEquals(query5[0].name, "Aidan");
  }

  @Test
  public void test_update() {
    Person[] people =
        new Person[] {
          new Person("Natalie", 43),
          new Person("Jeff", 42),
          new Person("Aidan", 21),
          new Person("Waverly Moua", 19)
        };
    Long[] insertedIds = Inquiry.get(INSTANCE_NAME).insert(Person.class).values(people).run();
    assertEquals(insertedIds.length, 4);

    people[2].age = 22;
    people[3].name = "Waverly Follestad";
    people[3].age = 20;
    Inquiry.get(INSTANCE_NAME).update(Person.class).values(people).run();

    Person[] query1 = Inquiry.get(INSTANCE_NAME).select(Person.class).sort("name").all();
    assertNotNull(query1);
    assertEquals(query1.length, 4);
    assertEquals(people[2].name, "Aidan");
    assertEquals(people[2].age, 22);
    assertEquals(people[3].name, "Waverly Follestad");
    assertEquals(people[3].age, 20);

    for (Person p : people) {
      p.id = 0;
      p.age = 1;
    }
    Inquiry.get(INSTANCE_NAME)
        .update(Person.class)
        .sort("name")
        .where("age < 43")
        .where("age > 20")
        .projection("age")
        .values(people)
        .run();

    Person[] query2 = Inquiry.get(INSTANCE_NAME).select(Person.class).sort("name").all();
    assertNotNull(query2);
    assertEquals(query2.length, 4);
    assertEquals(query2[0].name, "Aidan");
    assertEquals(query2[0].age, 1);
    assertEquals(query2[1].name, "Jeff");
    assertEquals(query2[1].age, 1);
    assertEquals(query2[2].name, "Natalie");
    assertEquals(query2[2].age, 43);
    assertEquals(query2[3].name, "Waverly Follestad");
    assertEquals(query2[3].age, 20);
  }

  @Test
  public void test_delete() {
    Person[] people =
        new Person[] {
          new Person("Natalie", 43),
          new Person("Jeff", 42),
          new Person("Aidan", 21),
          new Person("Waverly", 19)
        };
    Long[] insertedIds = Inquiry.get(INSTANCE_NAME).insert(Person.class).values(people).run();
    assertEquals(insertedIds.length, 4);

    Inquiry.get(INSTANCE_NAME).delete(Person.class).where("age > 21").run();

    Person[] query = Inquiry.get(INSTANCE_NAME).select(Person.class).sort("name").all();
    assertNotNull(query);
    assertEquals(query.length, 2);
    assertEquals(query[0].name, "Aidan");
    assertEquals(query[1].name, "Waverly");
  }

  @Test
  public void test_query_foreign_keys() {
    Person[] people = new Person[] {new Person("Natalie", 43), new Person("Angela", 41)};

    people[0].children.add(new Child("Aidan"));
    people[1].children.add(new Child("Dylan"));
    people[1].children.add(new Child("Elias"));

    Long[] insertedIds = Inquiry.get(INSTANCE_NAME).insert(Person.class).values(people).run();
    assertEquals(insertedIds.length, 2);

    Child[] insertedChildren = Inquiry.get(INSTANCE_NAME).select(Child.class).sort("name").all();
    assertNotNull("No children loaded from the database!", insertedChildren);
    assertEquals(insertedChildren.length, 3);
    assertEquals(insertedChildren[0].name, "Aidan");
    assertEquals(insertedChildren[0].parentId, (long) insertedIds[0]);
    assertEquals(insertedChildren[1].name, "Dylan");
    assertEquals(insertedChildren[1].parentId, (long) insertedIds[1]);
    assertEquals(insertedChildren[2].name, "Elias");
    assertEquals(insertedChildren[2].parentId, (long) insertedIds[1]);

    Person[] queriedPeople = Inquiry.get(INSTANCE_NAME).select(Person.class).sort("name").all();
    assertNotNull("No parent people loaded from the database!", queriedPeople);
    assertEquals(queriedPeople.length, 2);

    Person first = queriedPeople[0];
    assertEquals(first.name, "Angela");
    assertEquals(first.children.size(), 2);
    assertEquals(first.children.get(0).name, "Dylan");
    assertEquals(first.children.get(1).name, "Elias");

    Person second = queriedPeople[1];
    assertEquals(second.name, "Natalie");
    assertEquals(second.children.size(), 1);
    assertEquals(second.children.get(0).name, "Aidan");
  }

  @Test
  public void test_update_foreign_keys() {
    Person[] people = new Person[] {new Person("Natalie", 43), new Person("Angela", 41)};

    people[0].children.add(new Child("Aidan"));
    people[1].children.add(new Child("Dylan"));
    people[1].children.add(new Child("Elias"));

    Long[] insertedIds = Inquiry.get(INSTANCE_NAME).insert(Person.class).values(people).run();
    assertEquals(insertedIds.length, 2);

    Person[] queriedPeople = Inquiry.get(INSTANCE_NAME).select(Person.class).sort("name").all();
    assertNotNull(queriedPeople);
    queriedPeople[0].children.get(0).name += " Haddeland";
    queriedPeople[0].children.get(1).name += " Brasel";
    queriedPeople[1].children.get(0).name += " Follestad";

    Inquiry.get(INSTANCE_NAME).update(Person.class).values(queriedPeople).run();

    Person[] queriedPeople2 = Inquiry.get(INSTANCE_NAME).select(Person.class).sort("name").all();
    assertNotNull(queriedPeople2);
    assertEquals(queriedPeople[0].children.get(0).name, "Dylan Haddeland");
    assertEquals(queriedPeople[0].children.get(1).name, "Elias Brasel");
    assertEquals(queriedPeople[1].children.get(0).name, "Aidan Follestad");
  }

  @Test
  public void test_delete_foreign_keys() {
    Person[] people = new Person[] {new Person("Natalie", 43), new Person("Angela", 41)};

    people[0].children.add(new Child("Aidan"));
    people[1].children.add(new Child("Dylan"));
    people[1].children.add(new Child("Elias"));

    Long[] insertedIds = Inquiry.get(INSTANCE_NAME).insert(Person.class).values(people).run();
    assertEquals(insertedIds.length, 2);

    Person[] queriedPeople = Inquiry.get(INSTANCE_NAME).select(Person.class).sort("name").all();
    assertNotNull(queriedPeople);
    queriedPeople[0].children.remove(0);
    queriedPeople[1].children.remove(0);

    Inquiry.get(INSTANCE_NAME).update(Person.class).values(queriedPeople).run();

    Person[] queriedPeople2 = Inquiry.get(INSTANCE_NAME).select(Person.class).sort("name").all();
    assertNotNull(queriedPeople2);
    assertEquals(queriedPeople[0].children.size(), 1);
    assertEquals(queriedPeople[0].children.get(0).name, "Elias");
    assertEquals(queriedPeople[1].children.size(), 0);
  }

  @After
  public void cleanup() {
    Inquiry.destroy(INSTANCE_NAME);
  }
}
