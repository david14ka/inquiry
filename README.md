# Inquiry

Inquiry is a simple library for Android that makes construction and use of SQLite databases super easy.

Read and write class objects from tables in a database. Let Inquiry handle the heavy lifting.

---

# Gradle Dependency

[![jCenter](https://api.bintray.com/packages/drummer-aidan/maven/inquiry/images/download.svg)](https://bintray.com/drummer-aidan/maven/inquiry/_latestVersion)
[![Build Status](https://travis-ci.org/afollestad/inquiry.svg)](https://travis-ci.org/afollestad/inquiry)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

The Gradle dependency is available via [jCenter](https://bintray.com/drummer-aidan/maven/inquiry/view).
jCenter is the default Maven repository used by Android Studio.

### Dependency

Add this to your module's `build.gradle` file (make sure the version matches the last [release](https://github.com/afollestad/inquiry/releases/latest)):

```gradle
dependencies {
    // ... other dependencies
    compile 'com.afollestad:inquiry:3.2.2'
}
```

---

# Table of Contents

1. [Quick Setup](https://github.com/afollestad/inquiry#quick-setup)
2. [Instances](https://github.com/afollestad/inquiry#instances)
3. [Row Objects](https://github.com/afollestad/inquiry#row-objects)
4. [Table Reference Annotation](https://github.com/afollestad/inquiry#table-reference-annotation)
4. [Foreign Children Annotation](https://github.com/afollestad/inquiry#foreign-children-annotation)
5. [Querying Rows](https://github.com/afollestad/inquiry#querying-rows)
    1. [Basics](https://github.com/afollestad/inquiry#basics)
    2. [Where](https://github.com/afollestad/inquiry#where)
    2. [Where In and Where Not In](https://github.com/afollestad/inquiry#where-in-and-where-not-in)
    3. [Combining Where Statements](https://github.com/afollestad/inquiry#combining-where-statements)
    4. [Projection](https://github.com/afollestad/inquiry#projection)
    5. [Sorting and Limiting](https://github.com/afollestad/inquiry#sorting-and-limiting)
6. [Inserting Rows](https://github.com/afollestad/inquiry#inserting-rows)
7. [Updating Rows](https://github.com/afollestad/inquiry#updating-rows)
    1. [Basics](https://github.com/afollestad/inquiry#basics-1)
    2. [Projection](https://github.com/afollestad/inquiry#projection-1)
8. [Deleting Rows](https://github.com/afollestad/inquiry#deleting-rows)
9. [Dropping Tables](https://github.com/afollestad/inquiry#dropping-tables)
10. [Extra: Accessing Content Providers](https://github.com/afollestad/inquiry#extra-accessing-content-providers)
    1. [Setup](https://github.com/afollestad/inquiry#setup)
    2. [Basics](https://github.com/afollestad/inquiry#basics-2)

---

# Quick Setup

When your app starts, you need to initialize Inquiry. `init(Context, String, int)` and `destroy(Context)` 
can be used from anywhere that has a `Context`, but a reliable place to do so is in an Activity:

```java
public class MainActivity extends AppCompatActivity {

    @Override
    public void onResume() {
        super.onResume();
        
        // Creates an instance specifically for MainActivity
        Inquiry.newInstance(this, "my_new_database").build();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Checking for isFinishing() makes sure the Activity is actually closing.
        // onPause() can also be called when a Dialog opens, such as a permissions dialog.
        if (isFinishing()) {
            // Destroys only MainActivity's instance
            Inquiry.destroy(this);
        }
    }
}
```

---

# Instances

The static Inquiry instance that you access from this library is not always the same instance. For
thread safety and other reasons, Inquiry supports uses multiple instances.

---

In the small example in the section above, the use of the `newInstance` Builder creates a new instance for
`MainActivity`. It will access a local database called *"my_new_database"*.

The first parameter passed into `newInstance()` references `MainActivity`, which is an instance of `android.content.Context`.
This is used to access resources, but it is also used to keep track of the newly created instance which is later destroyed in `onPause()`.

If you wanted to keep track of instances with a custom string, you can:

```java
Inquiry.newInstance(this, "my_new_database")
    .instanceName("my_custom_instance")
    .build();
```

---

# Row Objects

In Inquiry, a row is just an object which contains a set of values that can be read from and written to
a table in your database. In a spreadsheet, a row goes from left to right; each cell is a column in the row.

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

# Table Reference Annotation

![ReferenceChart](https://raw.githubusercontent.com/afollestad/inquiry/master/art/reference.jpg)

In addition to the `@Column` annotation, Inquiry has a few special annotations that you can use. The
first is called `@Reference`. This annotation is used to link a field to another table, allowing you
to add a 3rd dimension to your class object storage.

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

**During insertion**, Inquiry will insert the value of the `spouse` Field into the table `spouses`. The value of
the `spouse` column in the current table will be set to the *_id* of the new row in the `spouses` table.

**During querying**, Inquiry will see the `@Reference` annotation, and do an automatic lookup for you.
The value of the `spouse` field is automatically pulled from the second table. It's similar to a JOIN in SQL.

**During deletion**, Inquiry will remove the value in the `spouse` field from the `spouses` table before
removing the parent row.

Basically, this allows you to have non-primitive column types that are blazing fast to insert or query. 
No serialization is necessary. You can even have two row objects which reference the same object.

---

# Foreign Children Annotation

![ForeignChildrenChart](https://raw.githubusercontent.com/afollestad/inquiry/master/art/foreignchildren.jpg)

Another annotation that Inquiry provides is `@ForeignChildren`. It's a bit like `@Reference`, but it allows
you to store a list of children in another table.

```java
public class Person {

    public Person() {
        // Default constructor is needed so Inquiry can auto construct instances
    }

    public Person(String name) {
        this.name = name;
    }

    @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
    public long id;
    @Column
    public String name;

    // inverseFieldName is optional, here it refers to the "parent" field in Child below
    @Reference(tableName = "children", foreignColumnName = "parentId", inverseFieldName = "parent")
    public List<Child> children;
}

public class Child {

    public Child() {
        // Default constructor is needed so Inquiry can auto construct instances
    }

    @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
    public long id;
    @Column
    public String name;
    @Column
    public long parentId;

    public Person parent;
}
```

Since `inverseFieldName` is set to point to the "parent" field in `Child`, `parent` will be set to a reference
of the parent `Person` object during queries.

---

**During insertion**, Inquiry will first insert the parent `Person` object. It's `_id` field will be
populated to the new row ID. It will then loop through `Person` objects inside of the `children` ArrayList,
and insert each object. The `parentId` of each child will be set to the `_id` of the parent object.

**During querying**, the parent object will be retrieved first. It will then retrieve all children which
have a `parentId` matching the parent object and populate the `children` ArrayList.

**During updating**, the parent object will be updated first. It will then update each child which is
present in the `children` ArrayList. Any rows in the foreign table that are *no longer* in the `children`
ArrayList will be deleted (which have a `parentId` matching the `_id` of the parent object).

**During deletion**, all children with a `parentId` matching the `_id` of the parent object will be deleted, followed
by the parent object.

Note that you are not limited to one level of foreign children. Foreign children can also have their own
foreign children.

---

# Querying Rows

### Basics

Querying a table retrieves rows, whether its every row in a table or rows that match a specific criteria.
Here's how you would retrieve all rows from a table called *"people"*:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Person[] result = Inquiry.get(this)
    .selectFrom("people", Person.class)
    .all();
```

If you only needed one row, using `one()` instead of `all()` is more efficient:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Person result = Inquiry.get(this)
    .selectFrom("people", Person.class)
    .one();
```

---

You can also perform the query on a separate thread using a callback:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Inquiry.get(this)
    .selectFrom("people", Person.class)
    .all(new GetCallback<Person>() {
        @Override
        public void result(Person[] result) {
            // Do something with result
        }
    });
```

Inquiry will automatically fill in your `@Column` fields with matching columns in each row of the table.
As mentioned in a previous section, `@Reference` fields are also automatically pulled from their reference table.

### Where

If you wanted to find rows with specific values in their columns, you can use `where` selection:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Person[] result = Inquiry.get(this)
    .selectFrom("people", Person.class)
    .where("name = ? AND age > ?", "Aidan", 21)
    .all();
```

The first parameter is a string, specifying two conditions that must be true (`AND` is used instead of `OR`).
The question marks are placeholders, which are replaced by the values you specify in the second comma-separated
vararg (or array) parameter.

---

If you wanted, you could skip using the question marks and only use one parameter:

```java
.where("name = 'Aidan' AND age > 21");
```

*However*, using the question marks and filler parameters can be easier to read if you're filling them in
with variables. Plus, this will automatically escape any strings that contain reserved SQL characters.

---

Inquiry includes a convenience method called `atPosition()` which lets you perform operations on a specific row
in your tables:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Person result = Inquiry.get(this)
    .selectFrom("people", Person.class)
    .atPosition(24)
    .one();
```

Behind the scenes, it's using `where(String)` to select the row. `atPosition()` moves to a row position 
and retrieves the row's `_id` column. So, tables need to have an `_id` column (which is unique for every row) 
for this method to work. `atPosition(int)` can even be used when updating or deleting, not just for selection.

---

### Where In and Where Not In

Here's basic usage of where-in:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Person[] result = Inquiry.get(this)
    .selectFrom("people", Person.class)
    .whereIn("age", 19, 21)
    .all();
```

The query above will retrieve any rows where the age is equal to `19` *or* `21`. You can pass an array
in place of `19, 21` too. **Note** that `whereIn` can be used with updating and deletion too.

---

### Combining Where Statements

You can combine multiple where and where-in statements together:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Person[] result = Inquiry.get(this)
    .selectFrom("people", Person.class)
    .where("age > 8")
    .where("age < 20")
    .orWhereIn("name", "Aidan", "Waverly")
    .all();
```

The above query translates to this where statement:

```bash
SELECT * FROM people WHERE age > 8 AND age < 20 OR name IN ('Aidan', 'Waverly')
```

It will retrieve all people in between ages 8 and 20, *or* anyone with the name Aidan or Waverly.
`where()` and `whereIn()` both have variations that begin with `or`.

---

### Projection

Projection allows you to only retrieve specific columns. Take this example, using the `Person` class
made in a section above:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Person[] result = Inquiry.get(this)
    .selectFrom("people", Person.class)
    .projection("age", "rank")
    .all();
```

This would retrieve all rows in the `people` table. Each row would only have the `age` and `rank` fields
populated; all other fields would have their default values (e.g. `null` or `0`).

One specific situation where projection is useful is if you were migrating a table to a new table that has more columns.
If you try to select columns that don't exist in a table, you'd get an error; for migration, you'd have to select only
the columns that exist in the source table. *Projection also makes queries fast and use less memory.*

---

### Sorting and Limiting

This code would limit the maximum number of rows returned to 100. It would sort the results by values
in the "name" column, in descending (Z-A, or greater to smaller) order:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Person[] result = Inquiry.get(this)
    .selectFrom("people", Person.class)
    .limit(100)
    .sortByDesc("name", "rank")
    .sortByAsc("age")
    .all();
```

The above would sort every row by name descending (large to small, Z-A) first, *then* by rank descending,
*and then* by age ascending (small to large). 100 rows would be returned, at the maximum.

If you prefer using a full SQL string for sorting, you can:

```
.sort("name DESC, rank DESC, age ASC")
```

# Inserting Rows

Insertion is pretty straight forward. This inserts three `People` into the table *"people"*:

```java
Person one = new Person("Waverly", 19, 8.9f, false);
Person two = new Person("Natalie", 42, 10f, false);
Person three = new Person("Aidan", 21, 5.7f, true);

// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Long[] insertedIds = Inquiry.get(this)
        .insertInto("people", Person.class)
        .values(one, two, three)
        .run();
```

Inquiry will automatically pull your `@Column` fields out and insert them into the table `people`.

Like `all()`, `run()` has a callback variation that will run the operation in a separate thread:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Inquiry.get(this)
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

### Basics

Updating is similar to insertion, however it results in changed rows rather than new rows. You can also use
`WHERE` statements like you can with querying.

```java
Person two = new Person("Natalie", 42, 10f, false);

// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Integer updatedCount = Inquiry.get(this)
    .update("people", Person.class)
    .values(two)
    .where("name = ?", "Aidan")
    .run();
```

The above will update all rows whose name is equal to *"Aidan"*, setting all columns to the values in the `Person`
object called `two`. If you didn't specify `where()` args, every row in the table would be updated.

---

Like querying, `atPosition(int)` can be used in place of `where(String)` to update a specific row.

### Projection

Sometimes, you don't want to change every column in a row when you update them. You can choose specifically
what columns you want to be changed using `projection`:

```java
Person two = new Person("Natalie", 42, 10f, false);

// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Integer updatedCount = Inquiry.get(this)
    .update("people", Person.class)
    .values(two)
    .where("name = ?", "Aidan")
    .projection("age", "rank")
    .run();
```

The above code will update any rows with their name equal to *"Aidan"*, however it will only modify
the `age` and `rank` columns of the updated rows. The other columns will be left alone.

# Deleting Rows

Deletion is simple:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Integer deletedCount = Inquiry.get(this)
    .deleteFrom("people")
    .where("age = ?", 21)
    .run();
```

The above code results in any rows with their age column set to *20* removed. If you didn't
specify `where()` args, every row in the table would be deleted.

---

Like querying, `atPosition(int)` can be used in place of `where(String)` to delete a specific row.

# Dropping Tables

Dropping a table means deleting it. It's pretty straight forward:

```java
// NOTE: if you pass a custom instance name rather than just a Context, pass the instance name into get() instead of a Context
Inquiry.get(this)
    .dropTable("people");
```

Just pass table name, and it's gone.

---

# Extra: Accessing Content Providers

Inquiry allows you to access content providers, which are basically external databases used in other apps.
A common usage of content providers is Android's MediaStore. Most local media players use content providers
to get a list of audio and video files scanned by the system; the system logs all of their meta data
so the title, duration, album art, etc. can be quickly accessed.

### Setup

Inquiry setup is still the same, but passing a database name is not required for content providers.

```java
public class MainActivity extends AppCompatActivity {

    @Override
    public void onResume() {
        super.onResume();
        Inquiry.newInstance(this, null).build();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isFinishing()) {
            Inquiry.destroy(this);
        }
    }
}
```

### Basics

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
Photo[] photos = Inquiry.get(this)
    .selectFrom(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Photo.class)
    .all();
```

Insert, update, and delete work the same way. Just pass that URI instead of a table name.