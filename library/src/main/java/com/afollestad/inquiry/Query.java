package com.afollestad.inquiry;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.inquiry.annotations.ForeignKey;
import com.afollestad.inquiry.callbacks.GetCallback;
import com.afollestad.inquiry.callbacks.RunCallback;
import com.afollestad.inquiry.lazyloading.LazyLoaderList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess")
@SuppressLint("DefaultLocale")
public class Query<RowType, RunReturn> {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SELECT, INSERT, UPDATE, DELETE})
    public @interface QueryType {
    }

    protected final static int SELECT = 1;
    protected final static int INSERT = 2;
    protected final static int UPDATE = 3;
    protected final static int DELETE = 4;

    private final Inquiry inquiryInstance;
    private Uri contentUri;
    private String tableName;
    @Nullable private final Class<RowType> rowClass;

    @QueryType private final int queryType;
    private String[] projection;
    private StringBuilder where;
    private List<String> whereArgs;
    private StringBuilder sortOrder;
    private int limit;
    private RowType[] values;

    private HashMap<Object, FieldDelegate> foreignChildren;

    Inquiry getInquiryInstance() {
        return inquiryInstance;
    }

    protected Query(@NonNull Inquiry inquiry, @NonNull Uri contentUri, @QueryType int type, @Nullable Class<RowType> mClass) {
        this.inquiryInstance = inquiry;
        this.contentUri = contentUri;
        if (this.contentUri.getScheme() == null || !this.contentUri.getScheme().equals("content"))
            throw new IllegalStateException("You can only use content:// URIs for content providers.");
        this.queryType = type;
        this.rowClass = mClass;
        this.foreignChildren = new HashMap<>(0);
    }

    protected Query(@NonNull Inquiry inquiry, @NonNull String tableName, @QueryType int type, @Nullable Class<RowType> mClass) {
        this.inquiryInstance = inquiry;
        this.queryType = type;
        this.rowClass = mClass;
        this.tableName = tableName;
        if (inquiry.databaseName == null)
            throw new IllegalStateException("Inquiry was not initialized with a database name, it can only use content providers in this configuration.");
        inquiry._getDatabase().createTableIfNecessary(tableName, mClass);
        foreignChildren = new HashMap<>(0);
    }

    private void appendWhere(String statement, String[] args, boolean or) {
        if (statement == null || statement.isEmpty()) return;
        int argCount = args != null ? args.length : 0;
        if (Utils.countOccurrences(statement, '?') != argCount)
            throw new IllegalArgumentException("There must be the same amount of args as there is '?' characters in your where statement.");
        if (where == null)
            where = new StringBuilder();
        if (whereArgs == null)
            whereArgs = new ArrayList<>(argCount);
        if (where.length() > 0)
            where.append(or ? " OR " : " AND ");
        where.append(statement);
        if (args != null)
            Collections.addAll(whereArgs, args);
    }

    private String getWhere() {
        return where != null ? where.toString() : null;
    }

    private String[] getWhereArgs() {
        return whereArgs != null && whereArgs.size() > 0 ?
                whereArgs.toArray(new String[whereArgs.size()]) : null;
    }

    private String getSort() {
        return sortOrder != null ? sortOrder.toString() : null;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> atPosition(@IntRange(from = 0,
            to = Integer.MAX_VALUE) int position) {
        Cursor cursor;
        if (contentUri != null) {
            cursor = inquiryInstance.context.getContentResolver().query(contentUri, null, getWhere(), getWhereArgs(), null);
        } else {
            if (inquiryInstance._getDatabase() == null)
                throw new IllegalStateException("Database helper was null.");
            else if (tableName == null)
                throw new IllegalStateException("Table name was null.");
            cursor = inquiryInstance._getDatabase().query(tableName, null, getWhere(), getWhereArgs(), null);
        }
        if (cursor != null) {
            if (position < 0 || position >= cursor.getCount()) {
                cursor.close();
                throw new IndexOutOfBoundsException(String.format("Position %d is out of bounds for cursor of size %d.",
                        position, cursor.getCount()));
            }
            if (!cursor.moveToPosition(position)) {
                cursor.close();
                throw new IllegalStateException(String.format("Unable to move to position %d in cursor of size %d.",
                        position, cursor.getCount()));
            }
            final int idIndex = cursor.getColumnIndex("_id");
            if (idIndex < 0) {
                cursor.close();
                throw new IllegalStateException("Didn't find a column named _id in this Cursor.");
            }
            int idValue = cursor.getInt(idIndex);
            appendWhere("_id = ?", new String[]{Integer.toString(idValue)}, false);
            cursor.close();
        }
        return this;
    }

    @NonNull
    @CheckResult
    private Query<RowType, RunReturn> where(@NonNull String selection, boolean or, @Nullable Object... selectionArgs) {
        appendWhere(selection, Utils.stringifyArray(selectionArgs), or);
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> where(@NonNull String selection, @Nullable Object... selectionArgs) {
        return where(selection, false, selectionArgs);
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> orWhere(@NonNull String selection, @Nullable Object... selectionArgs) {
        return where(selection, true, selectionArgs);
    }

    @NonNull
    @CheckResult
    private Query<RowType, RunReturn> whereIn(@NonNull String columnName, boolean or, @Nullable Object... selectionArgs) {
        if (selectionArgs == null || selectionArgs.length == 0)
            throw new IllegalArgumentException("You must specify non-null, non-empty selection args.");
        final String statement = String.format(Locale.getDefault(), "%s IN %s", columnName, Utils.createArgsString(selectionArgs.length));
        appendWhere(statement, Utils.stringifyArray(selectionArgs), or);
        return this;
    }

    @NonNull
    @CheckResult
    private Query<RowType, RunReturn> whereNotIn(@NonNull String columnName, boolean or, @Nullable Object... selectionArgs) {
        if (selectionArgs == null || selectionArgs.length == 0)
            throw new IllegalArgumentException("You must specify non-null, non-empty selection args.");
        final String statement = String.format(Locale.getDefault(), "%s NOT IN %s", columnName, Utils.createArgsString(selectionArgs.length));
        appendWhere(statement, Utils.stringifyArray(selectionArgs), or);
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> whereIn(@NonNull String columnName, @Nullable Object... selectionArgs) {
        return whereIn(columnName, false, selectionArgs);
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> orWhereIn(@NonNull String columnName, @Nullable Object... selectionArgs) {
        return whereIn(columnName, true, selectionArgs);
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> whereNotIn(@NonNull String columnName, @Nullable Object... selectionArgs) {
        return whereNotIn(columnName, false, selectionArgs);
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> orWhereNotIn(@NonNull String columnName, @Nullable Object... selectionArgs) {
        return whereNotIn(columnName, true, selectionArgs);
    }

    @NonNull
    public Query<RowType, RunReturn> clearWhere() {
        where.setLength(0);
        where = null;
        whereArgs.clear();
        whereArgs = null;
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> sort(@NonNull String sortOrder) {
        if (this.sortOrder == null)
            this.sortOrder = new StringBuilder(sortOrder.length());
        else if (this.sortOrder.length() > 0)
            this.sortOrder.append(", ");
        this.sortOrder.append(sortOrder);
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> sortByAsc(@NonNull String... columnNames) {
        if (sortOrder == null)
            sortOrder = new StringBuilder();
        sortOrder.append(Utils.join(sortOrder.length() > 0, "ASC", (Object[]) columnNames));
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> sortByDesc(@NonNull String... columnNames) {
        if (sortOrder == null)
            sortOrder = new StringBuilder();
        sortOrder.append(Utils.join(sortOrder.length() > 0, "DESC", (Object[]) columnNames));
        return this;
    }

    @NonNull
    public Query<RowType, RunReturn> clearSort() {
        if (sortOrder == null) return this;
        sortOrder.setLength(0);
        sortOrder = null;
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> limit(int limit) {
        this.limit = limit;
        return this;
    }

    @NonNull
    @CheckResult
    @SuppressWarnings("unchecked")
    protected final Query<RowType, RunReturn> value(@NonNull Object value) {
        values = (RowType[]) Array.newInstance(rowClass, 1);
        Array.set(values, 0, value);
        return this;
    }

    @NonNull
    @CheckResult
    protected final Query<RowType, RunReturn> valuesArray(@NonNull Object[] values) {
        //noinspection unchecked
        this.values = (RowType[]) values;
        return this;
    }

    @NonNull
    @CheckResult
    @SafeVarargs
    public final Query<RowType, RunReturn> values(@NonNull RowType... values) {
        this.values = values;
        return this;
    }

    @NonNull
    @CheckResult
    public final Query<RowType, RunReturn> values(@NonNull List<RowType> values) {
        if (values.size() == 0) {
            this.values = null;
            return this;
        }
        //noinspection unchecked
        this.values = (RowType[]) Array.newInstance(rowClass, values.size());
        for (int i = 0; i < values.size(); i++)
            this.values[i] = values.get(i);
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> projection(@NonNull String... values) {
        projection = values;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @CheckResult
    private RowType[] getInternal(int limit) {
        if (rowClass == null)
            return null;
        else if (inquiryInstance.context == null)
            return null;
        if (projection == null)
            projection = Converter.generateProjection(rowClass);

        String sort = getSort();
        if (limit > -1) {
            sort += String.format(Locale.getDefault(), " LIMIT %d", limit);
        }
        Cursor cursor;
        if (contentUri != null) {
            cursor = inquiryInstance.context.getContentResolver().query(
                    contentUri, projection, getWhere(), getWhereArgs(), sort);
        } else {
            if (inquiryInstance._getDatabase() == null)
                throw new IllegalStateException("Database helper was null.");
            else if (tableName == null)
                throw new IllegalStateException("Table name was null.");
            cursor = inquiryInstance._getDatabase().query(tableName,
                    projection, getWhere(), getWhereArgs(), sort);
        }

        if (cursor != null) {
            RowType[] results = null;
            try {
                if (cursor.getCount() > 0) {
                    results = (RowType[]) Array.newInstance(rowClass, cursor.getCount());
                    int index = 0;
                    while (cursor.moveToNext()) {
                        results[index] = Converter.cursorToObject(this, cursor, rowClass);
                        index++;
                    }
                }
            } finally {
                cursor.close();
            }
            return results;
        }
        return null;
    }

    @Nullable
    @CheckResult
    public RowType first() {
        if (rowClass == null) return null;
        RowType[] results = getInternal(1);
        if (results == null || results.length == 0)
            return null;
        return results[0];
    }

    @CheckResult
    public boolean any() {
        return first() != null;
    }

    @CheckResult
    public boolean any(AnyPredicate<RowType> predicate) {
        RowType[] rows = all();
        if (rows == null || rows.length == 0) return false;
        for (RowType r : rows) {
            if (predicate.match(r)) return true;
        }
        return false;
    }

    @CheckResult
    public boolean none() {
        return first() == null;
    }

    @CheckResult
    public boolean none(AnyPredicate<RowType> predicate) {
        RowType[] rows = all();
        if (rows == null || rows.length == 0) return true;
        for (RowType r : rows) {
            if (predicate.match(r)) return false;
        }
        return true;
    }

    @Nullable
    @CheckResult
    public RowType[] all() {
        return getInternal(limit > 0 ? limit : -1);
    }

    public void all(@NonNull final GetCallback<RowType> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final RowType[] results = all();
                if (inquiryInstance.handler == null) return;
                inquiryInstance.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.result(results);
                    }
                });
            }
        }).start();
    }

    @SuppressLint("SwitchIntDef")
    @SuppressWarnings("unchecked")
    public RunReturn run() {
        if (queryType != DELETE && (values == null || values.length == 0))
            throw new IllegalStateException("No values were provided for this query to run.");
        else if (inquiryInstance.context == null) {
            try {
                return (RunReturn) (Integer) 0;
            } catch (Throwable t) {
                return (RunReturn) (Long) 0L;
            }
        }

        final ContentResolver cr = inquiryInstance.context.getContentResolver();
        final List<FieldDelegate> clsProxies = Converter.classFieldDelegates(rowClass);
        if (tableName == null)
            throw new IllegalStateException("The table name cannot be null.");
        FieldDelegate rowIdProxy = inquiryInstance.getIdDelegate(rowClass);

        try {
            switch (queryType) {
                case INSERT:
                    Long[] insertedIds = new Long[values.length];
                    if (inquiryInstance._getDatabase() != null) {
                        for (int i = 0; i < values.length; i++) {
                            final RowType row = values[i];
                            if (row == null) continue;
                            RowValues rowValues = Converter.classToValues(
                                    row, null, clsProxies, foreignChildren);
                            insertedIds[i] = inquiryInstance._getDatabase().insert(tableName,
                                    rowValues.toContentValues());
                            if (rowIdProxy != null)
                                rowIdProxy.set(row, insertedIds[i]);
                        }
                    } else if (contentUri != null) {
                        for (int i = 0; i < values.length; i++) {
                            final RowType row = values[i];
                            if (row == null) continue;
                            RowValues rowValues = Converter.classToValues(
                                    row, null, clsProxies, foreignChildren);
                            final Uri uri = cr.insert(contentUri, rowValues.toContentValues());
                            if (uri == null) return (RunReturn) (Long) (-1L);
                            insertedIds[i] = Long.parseLong(uri.getLastPathSegment());
                            if (rowIdProxy != null)
                                rowIdProxy.set(row, insertedIds[i]);
                        }
                    } else
                        throw new IllegalStateException("Database helper was null.");
                    postRun(false);
                    return (RunReturn) insertedIds;
                case UPDATE: {
                    boolean allHaveIds = rowIdProxy != null;
                    if (rowIdProxy != null && values != null) {
                        for (RowType rowValue : values) {
                            if (rowValue == null) continue;
                            Long id = rowIdProxy.get(rowValue);
                            if (id == null || id <= 0) {
                                allHaveIds = false;
                                break;
                            }
                        }
                    }

                    if (allHaveIds) {
                        // We want to update each object as themselves
                        if (getWhere() != null && !getWhere().trim().isEmpty()) {
                            throw new IllegalStateException("You want to update rows which have IDs, " +
                                    "but specified a where statement.");
                        }

                        int updatedCount = 0;
                        for (RowType row : values) {
                            if (row == null) continue;
                            Long rowId = rowIdProxy.get(row);
                            RowValues rowValues = Converter.classToValues(row,
                                    projection, clsProxies, foreignChildren);
                            ContentValues values = rowValues.toContentValues();
                            if (inquiryInstance._getDatabase() != null) {
                                updatedCount += inquiryInstance._getDatabase().update(tableName,
                                        values, "_id = ?", new String[]{rowId + ""});
                            } else if (contentUri != null) {
                                updatedCount += cr.update(contentUri, values, "_id = ?",
                                        new String[]{rowId + ""});
                            } else
                                throw new IllegalStateException("Database helper was null.");
                        }

                        postRun(true);
                        return (RunReturn) (Integer) updatedCount;
                    }

                    RowType firstNotNull = values[values.length - 1];
                    if (firstNotNull == null) {
                        for (int i = values.length - 2; i >= 0; i--) {
                            firstNotNull = values[i];
                            if (firstNotNull != null) break;
                        }
                    }
                    if (firstNotNull == null)
                        throw new IllegalStateException("No non-null values specified to update.");

                    RowValues rowValues = Converter.classToValues(firstNotNull,
                            projection, clsProxies, foreignChildren);
                    ContentValues values = rowValues.toContentValues();
                    if (inquiryInstance._getDatabase() != null) {
                        RunReturn value = (RunReturn) (Integer) inquiryInstance._getDatabase()
                                .update(tableName, values, getWhere(), getWhereArgs());
                        postRun(true);
                        return value;
                    } else if (contentUri != null)
                        return (RunReturn) (Integer) cr.update(contentUri, values, getWhere(), getWhereArgs());
                    else
                        throw new IllegalStateException("Database helper was null.");
                }
                case DELETE: {
                    Long[] idsToDelete = null;
                    if (rowIdProxy != null && values != null) {
                        int nonNullFound = 0;
                        idsToDelete = new Long[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] == null) continue;
                            nonNullFound++;
                            Long id = rowIdProxy.get(values[i]);
                            idsToDelete[i] = id;
                            if (id == null || id <= 0) {
                                idsToDelete = null;
                                break;
                            }
                        }
                        if (nonNullFound == 0) idsToDelete = null;
                    }

                    if (idsToDelete != null) {
                        // We want to update each object as themselves
                        if (getWhere() != null && !getWhere().trim().isEmpty()) {
                            throw new IllegalStateException("You want to delete rows which have IDs, but specified a where statement.");
                        }
                        //noinspection CheckResult,ConfusingArgumentToVarargsMethod
                        whereIn("_id", idsToDelete);
                    }

                    if (inquiryInstance._getDatabase() != null) {
                        RowType[] rowsThatWillDelete = all();
                        RunReturn value = (RunReturn) (Integer) inquiryInstance._getDatabase().delete(tableName, getWhere(), getWhereArgs());
                        traverseDelete(rowsThatWillDelete);
                        return value;
                    } else if (contentUri != null)
                        return (RunReturn) (Integer) cr.delete(contentUri, getWhere(), getWhereArgs());
                    else
                        throw new IllegalStateException("Database helper was null.");
                }
            }
        } catch (Throwable t) {
            Utils.wrapInReIfNecessary(t);
        }
        return null;
    }

    public void run(@NonNull final RunCallback<RunReturn> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final RunReturn changed = Query.this.run();
                if (inquiryInstance.handler == null) return;
                inquiryInstance.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.result(changed);
                    }
                });
            }
        }).start();
    }

    private void traverseDelete(RowType[] rowsThatWillDelete) {
        if (rowsThatWillDelete == null || rowsThatWillDelete.length == 0) return;
        List<FieldDelegate> proxies = Converter.classFieldDelegates(rowClass);

        for (RowType row : rowsThatWillDelete) {
            for (FieldDelegate proxy : proxies) {
                ForeignKey fkAnn = proxy.getForeignKey();
                if (fkAnn != null) {
                    try {
                        FieldDelegate rowIdProxy = inquiryInstance.getIdDelegate(rowClass);
                        if (rowIdProxy == null)
                            throw new IllegalStateException("No _id column field found in " + rowClass);
                        Class<?> listGenericType = Utils.getGenericTypeOfProxy(proxy);
                        Long rowId = rowIdProxy.get(row);
                        Inquiry fkInstance = Inquiry.copy(inquiryInstance, "[@fk]:" + fkAnn.tableName() + "//" + fkAnn.foreignColumnName(), false);
                        fkInstance.deleteFrom(fkAnn.tableName(), listGenericType)
                                .where(fkAnn.foreignColumnName() + " = ?", rowId)
                                .run();
                        fkInstance.destroyInstance();
                    } catch (Throwable t) {
                        Utils.wrapInReIfNecessary(t);
                    }
                }
            }
        }
    }

    private void postRun(boolean updateMode) {
        if (foreignChildren == null || foreignChildren.size() == 0) return;
        for (Object row : foreignChildren.keySet()) {
            FieldDelegate proxy = foreignChildren.get(row);
            postRun(updateMode, row, proxy);
        }
    }

    private void postRun(boolean updateMode, Object row, FieldDelegate proxy) {
        try {
            ForeignKey fkAnn = proxy.getForeignKey();
            Object fldVal = proxy.get(row);

            if (updateMode && Utils.classExtendsLazyLoader(proxy.getType())) {
                LazyLoaderList lazyLoader = (LazyLoaderList) fldVal;
                if (lazyLoader == null || !lazyLoader.didLazyLoad()) {
                    // Lazy loading didn't happen, nothing was populated, so nothing changed
                    return;
                }
            }

            Class<?> listGenericType = Utils.getGenericTypeOfProxy(proxy);
            FieldDelegate rowIdProxy = inquiryInstance.getIdDelegate(row.getClass());
            FieldDelegate foreignKeyIdProxy = inquiryInstance.getIdDelegate(listGenericType);
            FieldDelegate foreignKeyProxy = Converter.getProxyByName(
                    Converter.classFieldDelegates(listGenericType),
                    fkAnn.foreignColumnName(), Long.class, long.class);

            if (rowIdProxy == null)
                throw new IllegalStateException("You cannot use the @ForeignKey annotation on a field within a class that doesn't have an _id column.");
            if (foreignKeyIdProxy == null)
                throw new IllegalStateException("The @ForeignKey annotation can only be used on fields which contain class objects that have an _id column, " + listGenericType + " does not.");
            if (foreignKeyProxy == null)
                throw new IllegalStateException("The @ForeignKey annotation on " + proxy.name() + " references a non-existent column (or a column which can't hold an Int64 ID): " + fkAnn.foreignColumnName());

            Long rowId = rowIdProxy.get(row);
            if (rowId == null || rowId <= 0)
                throw new IllegalStateException("The current row's ID is 0, you cannot insert/update @ForeignKey fields if the parent class has no ID.");

            List list = null;
            Object[] array = null;

            if (fldVal != null) {
                if (proxy.getType().isArray())
                    //noinspection ConstantConditions
                    array = (Object[]) fldVal;
                else if (Utils.classImplementsList(proxy.getType()))
                    list = (List) fldVal;
                else
                    array = new Object[]{fldVal};
            }
            Inquiry fkInstance = Inquiry.copy(inquiryInstance, "[@fk]:" + fkAnn.tableName() + "//" + fkAnn.foreignColumnName(), false);

            if ((array != null && array.length > 0) || (list != null && list.size() > 0)) {
                // Update foreign row columns with this row's ID
                if (array != null) {
                    for (Object child : array)
                        foreignKeyProxy.set(child, rowId);
                } else {
                    for (int i = 0; i < list.size(); i++)
                        foreignKeyProxy.set(list.get(i), rowId);
                }

                if (updateMode) {
                    // Delete any rows in the foreign table which reference this row
                    fkInstance.deleteFrom(fkAnn.tableName(), listGenericType)
                            .where(fkAnn.foreignColumnName() + " = ?", rowId)
                            .run();
                }

                // Insert rows from this field into the foreign table
                if (array != null) {
                    fkInstance.insertInto(fkAnn.tableName(), listGenericType)
                            .valuesArray(array)
                            .run();
                } else {
                    fkInstance.insertInto(fkAnn.tableName(), listGenericType)
                            .values(list)
                            .run();
                }
            } else {
                // Delete any rows in the foreign table which reference this row
                fkInstance.deleteFrom(fkAnn.tableName(), listGenericType)
                        .where(fkAnn.foreignColumnName() + " = ?", rowId)
                        .run();
            }

            fkInstance.destroyInstance();
        } catch (Throwable t) {
            Utils.wrapInReIfNecessary(t);
        }
    }
}
