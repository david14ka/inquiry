# Inquiry

Inquiry is a simple library for Android that makes construction and use of SQLite databases super easy.

Read and write class objects from tables in a database. Let Inquiry handle the heavy lifting.

---

# Gradle Dependency

[ ![jCenter](https://api.bintray.com/packages/drummer-aidan/maven/inquiry/images/download.svg) ](https://bintray.com/drummer-aidan/maven/inquiry/_latestVersion)
[![Build Status](https://travis-ci.org/afollestad/inquiry.svg)](https://travis-ci.org/afollestad/inquiry)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

The Gradle dependency is available via [jCenter](https://bintray.com/drummer-aidan/maven/material-camera/view).
jCenter is the default Maven repository used by Android Studio.

### Dependency

Add this to your module's `build.gradle` file (make sure the version matches the version on the JitPack badge above):

```gradle
dependencies {
    ...
    compile 'com.github.afollestad:inquiry:2.0.1'
}
```

---

# Table of Contents

1. [Quick Setup](https://github.com/afollestad/inquiry#quick-setup)
2. [Example Row](https://github.com/afollestad/inquiry#example-row)
3. [References](https://github.com/afollestad/inquiry#references)
4. [Querying Rows](https://github.com/afollestad/inquiry#querying-rows)
    1. [Basics](https://github.com/afollestad/inquiry#basics)
    2. [Where](https://github.com/afollestad/inquiry#wheren)
    3. [Sorting and Limiting](https://github.com/afollestad/inquiry#sorting-and-limiting)
5. [Inserting Rows](https://github.com/afollestad/inquiry#inserting-rows)
6. [Updating Rows](https://github.com/afollestad/inquiry#updating-rows)
    1. [Basics](https://github.com/afollestad/inquiry#basics-1)
    2. [Updating Specific Columns](https://github.com/afollestad/inquiry#updating-specific-columns)
7. [Deleting Rows](https://github.com/afollestad/inquiry#deleting-rows)
8. [Dropping Tables](https://github.com/afollestad/inquiry#dropping-tables)
9. [Extra: Accessing Content Providers](https://github.com/afollestad/inquiry#extra-accessing-content-providers)
    1. [Initialization](https://github.com/afollestad/inquiry#initialization)
    2. [Basics](https://github.com/afollestad/inquiry#basics-2)

---

# Quick Setup

When your app starts, you need to initialize Inquiry. `init()` and `deinit()` can be used from anywhere, but a reliable place to do so is in an Activity:

```java
public class MainActivity extends AppCompatActivity {

    @Override
    public void onResume() {
        super.onResume();
        Inquiry.init(this, "myDatabase", 1);
    }

    @Override
    public void onPause() {
        super.onPause();
        Inquiry.deinit();
    }
}
```

`init()` takes a `Context` in the first parameter, and the name of the database that'll you be using
in the second parameter. The third parameter is the database version, which could always be '1' if you want. 
Incrementing the number will drop tables created with a lower number next time they are accessed.

Think of a database like a file that contains a set of tables (a table is basically
a spreadsheet; it contains rows and columns).

When your app is done with Inquiry, you *should* call `deinit()` to help clean up references and avoid memory leaks.

---

# Example Row

In Inquiry, a row is just an object which contains a set of values that can be read from and written to
a table in your database.

```java
public class Person {

    public Person() {
        // Default constructor is needed so Inquiry can auto construct instances
    }

    public Person(String name, int age, float rank, boolean admin, Person spouse) {
        this.name = name;
        this.age = age;
        this.rank = rank;
        this.admin = admin;
        this.spouse = spouse;
    }

    @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
    public long id;
    @Column
    public String name;
    @Column
    public int age;
    @Column
    public float rank;
    @Column
    public boolean admin;
    
    // Reference annotation is discussed in the next section
    @Reference(columnName = "spouse", tableName = "spouses")
    public Person spouse;
}
```

Notice that all the fields are annotated with the `@Column` annotation. If you have fields without that
annotation, they will be ignored by Inquiry.

Notice that the `_id` field contains optional parameters in its annotation:

* `name` indicates a column name, if the column name is different than what you name the class field.
* `primaryKey` indicates its column is the main column used to identify the row. No other row in the
table can have the same value for that specific column. This is commonly used with IDs.
* `notNull` indicates that you can never insert null as a value for that column.
* `autoIncrement` indicates that you don't manually set the value of this column. Every time
you insert a row into the table, this column will be incremented by one automatically. This can
only be used with INTEGER columns (short, int, or long fields), however.

---

# References (Coming Soon)

In addition to the `@Column` annotation, Inquiry has a special annotation called `@Reference`. This 
annotation is used to link a field to another table.

Let's take the `Person` class from the previous section, but simplify it a bit:

```java
public class Person {

    public Person() {
        // Default constructor is needed so Inquiry can auto construct instances
    }

    public Person(String name, Person spouse) {
        this.name = name;
        this.spouse = spouse;
    }

    @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
    public long id;
    @Column
    public String name;
    
    // Column name is optional, it will use the name of the field by default
    @Reference(columnName = "spouse", tableName = "spouses")
    public Person spouse;
}
```

**During insertion**, Inquiry will insert the `spouse` Field into the table `spouses`. The value of 
the `spouse` column in the current table will be set to the *_id* of the new row in the `spouses` table.

**During querying**, Inquiry will see the `@Reference` annotation, and do an automatic lookup for you.
The value of the `spouse` field is automatically pulled from the second table into the current table.

Basically, this allows you to have non-primitive column types that are blazing fast to insert or query. 
No serialization is necessary. You can even have two rows which reference the same object (a single object 
with the same `_id` value).

---

# Querying Rows

#### Basics

Querying retrieves rows, whether its every row in a table or rows that match a specific criteria.
Here's how you would retrieve all rows from a table called *"people"*:

```java
Person[] result = Inquiry.get()
    .selectFrom("people", Person.class)
    .all();
```

If you only needed one row, using `one()` instead of `all()` is more efficient:

```java
Person result = Inquiry.get()
    .selectFrom("people", Person.class)
    .one();
```

---

You can also perform the query on a separate thread using a callback:

```java
Inquiry.get()
    .selectFrom("people", Person.class)
    .all(new GetCallback<Person>() {
        @Override
        public void result(Person[] result) {
            // Do something with result
        }
    });
```

Inquiry will automatically fill in your `@Column` fields with matching columns in each row of the table.

#### Where

If you wanted to find rows with specific values in their columns, you can use `where` selection:

```java
Person[] result = Inquiry.get()
    .selectFrom("people", Person.class)
    .where("name = ? AND age = ?", "Aidan", 20)
    .all();
```

The first parameter is a string, specifying two conditions that must be true (`AND` is used instead of `OR`).
The question marks are placeholders, which are replaced by the values you specify in the second comma-separated
vararg (or array) parameter.

---

If you wanted, you could skip using the question marks and only use one parameter:

```java
.where("name = 'Aidan' AND age = 20");
```

*However*, using the question marks and filler parameters can be easier to read if you're filling them in
with variables. Plus, this will automatically escape any strings that contain reserved SQL characters.

---

Inquiry includes a convenience method called `atPosition()` which lets you perform operations on a specific row
in your tables:

```java
Person result = Inquiry.get()
    .selectFrom("people", Person.class)
    .atPosition(24)
    .one();
```

Behind the scenes, it's using `where(String)` to select the row. `atPosition()` moves to a row position 
and retrieves the row's `_id` column. So, tables need to have an `_id` column (which is unique for every row) 
for this method to work.

#### Sorting and Limiting

This code would limit the maximum number of rows returned to 100. It would sort the results by values
in the "name" column, in descending (Z-A, or greater to smaller) order:

```java
Person[] result = Inquiry.get()
    .selectFrom("people", Person.class)
    .limit(100)
    .sort("name DESC")
    .all();
```

If you understand SQL, you'll know you can specify multiple sort parameters separated by commas.

```java
.sort("name DESC, age ASC");
```

The above sort value would sort every column by name descending (large to small, Z-A) first, *and then* by age ascending (small to large).

# Inserting Rows

Insertion is pretty straight forward. This inserts three `People` into the table *"people"*:

```java
Person one = new Person("Waverly", 18, 8.9f, false);
Person two = new Person("Natalie", 42, 10f, false);
Person three = new Person("Aidan", 20, 5.7f, true);

Long[] insertedIds = Inquiry.get()
        .insertInto("people", Person.class)
        .values(one, two, three)
        .run();
```

Inquiry will automatically pull your `@Column` fields out and insert them into the table `people`.

Like `all()`, `run()` has a callback variation that will run the operation in a separate thread:

```java
Inquiry.get()
    .insertInto("people", Person.class)
    .values(one, two, three)
    .run(new RunCallback() {
        @Override
        public void result(Long[] insertedIds) {
            // Do something
        }
    });
```

If your row class contains a field called `_id` with `autoIncrement` set to true, this field will 
automatically be updated to a newly inserted row ID.

# Updating Rows

#### Basics

Updating is similar to insertion, however it results in changed rows rather than new rows:

```java
Person two = new Person("Natalie", 42, 10f, false);

Integer updatedCount = Inquiry.get()
    .update("people", Person.class)
    .values(two)
    .where("name = ?", "Aidan")
    .run();
```

The above will update all rows whose name is equal to *"Aidan"*, setting all columns to the values in the `Person`
object called `two`. If you didn't specify `where()` args, every row in the table would be updated.

---

Like querying, `atPosition(int)` can be used in place of `where(String)` to update a specific row.

#### Updating Specific Columns

Sometimes, you don't want to change every column in a row when you update them. You can choose specifically
what columns you want to be changed using `onlyUpdate`:

```java
Person two = new Person("Natalie", 42, 10f, false);

Integer updatedCount = Inquiry.get()
    .update("people", Person.class)
    .values(two)
    .where("name = ?", "Aidan")
    .onlyUpdate("age", "rank")
    .run();
```

The above code will update any rows with their name equal to *"Aidan"*, however it will only modify
the `age` and `rank` columns of the updated rows. The other columns will be left alone.

# Deleting Rows

Deletion is simple:

```java
Integer deletedCount = Inquiry.get()
    .deleteFrom("people")
    .where("age = ?", 20)
    .run();
```

The above code results in any rows with their age column set to *20* removed. If you didn't
specify `where()` args, every row in the table would be deleted.

---

Like querying, `atPosition(int)` can be used in place of `where(String)` to delete a specific row.

# Dropping Tables

Dropping a table means deleting it. It's pretty straight forward:

```java
Inquiry.get()
    .dropTable("people");
```

Just pass table name, and it's gone.

---

# Extra: Accessing Content Providers

Inquiry allows you to access content providers, which are basically external databases used in other apps.
A common usage of content providers is Android's MediaStore. Most local media players use content providers
to get a list of audio and video files scanned by the system; the system logs all of their meta data
so the title, duration, album art, etc. can be quickly accessed.

#### Initialization

Inquiry initialization is still the same, but passing a database name is not required for content providers.

```java
public class MainActivity extends AppCompatActivity {

    @Override
    public void onResume() {
        super.onResume();
        Inquiry.init(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Inquiry.deinit();
    }
}
```

#### Basics

This small example will read artists (for songs) on your phone. Here's the row class:

```java
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
```

You can perform all the same operations, but you pass a `content://` URI instead of a table name:

```java
Photo[] photos = Inquiry.get()
    .selectFrom(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Photo.class)
    .all();
```

Insert, update, and delete work the same way. Just pass that URI.