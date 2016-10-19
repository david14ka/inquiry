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

import com.afollestad.inquiry.callbacks.GetCallback;
import com.afollestad.inquiry.callbacks.RunCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressLint("DefaultLocale")
public final class Query<RowType, RunReturn> {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SELECT, INSERT, UPDATE, DELETE})
    public @interface QueryType {
    }

    protected final static int SELECT = 1;
    protected final static int INSERT = 2;
    protected final static int UPDATE = 3;
    protected final static int DELETE = 4;

    private final Inquiry mInquiry;
    private Uri mContentUri;
    @Nullable
    private final Class<RowType> mRowClass;
    @Nullable
    private SQLiteHelper mDatabase;

    @QueryType
    private final int mQueryType;
    private String[] mProjection;
    private StringBuilder mWhere;
    private List<String> mWhereArgs;
    private StringBuilder mSortOrder;
    private int mLimit;
    private RowType[] mValues;

    protected Query(@NonNull Inquiry inquiry, @NonNull Uri contentUri, @QueryType int type, @Nullable Class<RowType> mClass) {
        mInquiry = inquiry;
        mContentUri = contentUri;
        if (mContentUri.getScheme() == null || !mContentUri.getScheme().equals("content"))
            throw new IllegalStateException("You can only use content:// URIs for content providers.");
        mQueryType = type;
        mRowClass = mClass;
    }

    protected Query(@NonNull Inquiry inquiry, @NonNull String tableName, @QueryType int type, @Nullable Class<RowType> mClass, int databaseVersion) {
        mInquiry = inquiry;
        mQueryType = type;
        mRowClass = mClass;
        if (inquiry.mDatabaseName == null)
            throw new IllegalStateException("Inquiry was not initialized with a database name, it can only use content providers in this configuration.");
        mDatabase = new SQLiteHelper(inquiry.mContext, inquiry.mDatabaseName,
                tableName, ClassRowConverter.getClassSchema(mClass), databaseVersion);
    }

    private void appendWhere(String statement, String[] args, boolean or) {
        final int argCount = args != null ? args.length : 0;
        if (Utils.countOccurrences(statement, '?') != argCount)
            throw new IllegalArgumentException("There must be the same amount of args as there is '?' characters in your where statement.");
        if (mWhere == null)
            mWhere = new StringBuilder();
        if (mWhereArgs == null)
            mWhereArgs = new ArrayList<>(argCount);
        if (mWhere.length() > 0)
            mWhere.append(or ? " OR " : " AND ");
        mWhere.append(statement);
        if (args != null)
            Collections.addAll(mWhereArgs, args);
    }

    private String getWhere() {
        return mWhere != null ? mWhere.toString() : null;
    }

    private String[] getWhereArgs() {
        return mWhereArgs != null && mWhereArgs.size() > 0 ?
                mWhereArgs.toArray(new String[mWhereArgs.size()]) : null;
    }

    private String getSort() {
        return mSortOrder != null ? mSortOrder.toString() : null;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> atPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int position) {
        Cursor cursor;
        if (mContentUri != null) {
            cursor = mInquiry.mContext.getContentResolver().query(mContentUri, null, getWhere(), getWhereArgs(), null);
        } else {
            if (mDatabase == null) throw new IllegalStateException("Database helper was null.");
            cursor = mDatabase.query(null, getWhere(), getWhereArgs(), null);
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
            final int idValue = cursor.getInt(idIndex);
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
        return where(selection, false, selectionArgs);
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
    public Query<RowType, RunReturn> whereIn(@NonNull String columnName, @Nullable Object... selectionArgs) {
        return whereIn(columnName, false, selectionArgs);
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> orWhereIn(@NonNull String columnName, @Nullable Object... selectionArgs) {
        return whereIn(columnName, true, selectionArgs);
    }

    @NonNull
    public Query<RowType, RunReturn> clearWhere() {
        mWhere.setLength(0);
        mWhere = null;
        mWhereArgs.clear();
        mWhereArgs = null;
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> sort(@NonNull String sortOrder) {
        if (mSortOrder == null)
            mSortOrder = new StringBuilder(sortOrder.length());
        else if (mSortOrder.length() > 0)
            mSortOrder.append(", ");
        mSortOrder.append(sortOrder);
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> sortByAsc(@NonNull String... columnNames) {
        if (mSortOrder == null)
            mSortOrder = new StringBuilder();
        mSortOrder.append(Utils.join(mSortOrder.length() > 0, "ASC", columnNames));
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> sortByDesc(@NonNull String... columnNames) {
        if (mSortOrder == null)
            mSortOrder = new StringBuilder();
        mSortOrder.append(Utils.join(mSortOrder.length() > 0, "DESC", columnNames));
        return this;
    }

    @NonNull
    public Query<RowType, RunReturn> clearSort() {
        if (mSortOrder == null) return this;
        mSortOrder.setLength(0);
        mSortOrder = null;
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> limit(int limit) {
        mLimit = limit;
        return this;
    }

    @NonNull
    @CheckResult
    @SuppressWarnings("unchecked")
    protected final Query<RowType, RunReturn> value(@NonNull Object value) {
        mValues = (RowType[]) Array.newInstance(mRowClass, 1);
        Array.set(mValues, 0, value);
        return this;
    }

    @NonNull
    @CheckResult
    @SafeVarargs
    public final Query<RowType, RunReturn> values(@NonNull RowType... values) {
        mValues = values;
        return this;
    }

    @NonNull
    @CheckResult
    public Query<RowType, RunReturn> projection(@NonNull String... values) {
        mProjection = values;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @CheckResult
    private RowType[] getInternal(int limit) {
        if (mRowClass == null)
            return null;
        else if (mInquiry.mContext == null)
            return null;
        if (mProjection == null)
            mProjection = ClassRowConverter.generateProjection(mRowClass);
        if (mQueryType == SELECT) {
            String sort = getSort();
            if (limit > -1) sort += String.format(Locale.getDefault(), " LIMIT %d", limit);
            Cursor cursor;
            if (mContentUri != null) {
                cursor = mInquiry.mContext.getContentResolver().query(mContentUri, mProjection, getWhere(), getWhereArgs(), sort);
            } else {
                if (mDatabase == null) throw new IllegalStateException("Database helper was null.");
                cursor = mDatabase.query(mProjection, getWhere(), getWhereArgs(), sort);
            }
            if (cursor != null) {
                RowType[] results = null;
                if (cursor.getCount() > 0) {
                    results = (RowType[]) Array.newInstance(mRowClass, cursor.getCount());
                    int index = 0;
                    while (cursor.moveToNext()) {
                        results[index] = ClassRowConverter.cursorToCls(mInquiry, cursor, mRowClass);
                        index++;
                    }
                }
                cursor.close();
                close();
                return results;
            }
        }
        close();
        return null;
    }

    @Nullable
    @CheckResult
    public RowType one() {
        if (mRowClass == null) return null;
        RowType[] results = getInternal(1);
        if (results == null || results.length == 0)
            return null;
        return results[0];
    }

    @CheckResult
    public boolean any() {
        return one() != null;
    }

    @CheckResult
    public boolean any(AnyPredicate<RowType> predicate) {
        final RowType[] rows = all();
        if (rows == null || rows.length == 0) return false;
        for (RowType r : rows) {
            if (predicate.match(r)) return true;
        }
        return false;
    }

    @CheckResult
    public boolean none() {
        return one() == null;
    }

    @CheckResult
    public boolean none(AnyPredicate<RowType> predicate) {
        final RowType[] rows = all();
        if (rows == null || rows.length == 0) return true;
        for (RowType r : rows) {
            if (predicate.match(r)) return false;
        }
        return true;
    }

    @Nullable
    @CheckResult
    public RowType[] all() {
        return getInternal(mLimit > 0 ? mLimit : -1);
    }

    public void all(@NonNull final GetCallback<RowType> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final RowType[] results = all();
                if (mInquiry.mHandler == null) return;
                mInquiry.mHandler.post(new Runnable() {
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
        if (mQueryType != DELETE && (mValues == null || mValues.length == 0))
            throw new IllegalStateException("No values were provided for this query to run.");
        else if (mInquiry.mContext == null) {
            try {
                return (RunReturn) (Integer) 0;
            } catch (Throwable t) {
                return (RunReturn) (Long) 0L;
            }
        }
        final ContentResolver cr = mInquiry.mContext.getContentResolver();
        final List<Field> clsFields = ClassRowConverter.getAllFields(mRowClass);
        switch (mQueryType) {
            case INSERT:
                final Field idField = ClassRowConverter.getIdField(clsFields);
                Long[] insertedIds = new Long[mValues.length];
                if (mDatabase != null) {
                    for (int i = 0; i < mValues.length; i++) {
                        final RowType row = mValues[i];
                        insertedIds[i] = mDatabase.insert(ClassRowConverter.clsToVals(mInquiry, row, null, clsFields, false));
                        ClassRowConverter.setIdField(row, idField, insertedIds[i]);
                    }
                } else if (mContentUri != null) {
                    for (int i = 0; i < mValues.length; i++) {
                        final RowType row = mValues[i];
                        final Uri uri = cr.insert(mContentUri, ClassRowConverter.clsToVals(mInquiry, row, null, clsFields, false));
                        if (uri == null) return (RunReturn) (Long) (-1L);
                        insertedIds[i] = Long.parseLong(uri.getLastPathSegment());
                        ClassRowConverter.setIdField(row, idField, insertedIds[i]);
                    }
                } else
                    throw new IllegalStateException("Database helper was null.");
                close();
                return (RunReturn) insertedIds;
            case UPDATE: {
                final ContentValues values = ClassRowConverter.clsToVals(mInquiry, mValues[mValues.length - 1], mProjection, clsFields, true);
                if (mDatabase != null) {
                    RunReturn value = (RunReturn) (Integer) mDatabase.update(values, getWhere(), getWhereArgs());
                    close();
                    return value;
                } else if (mContentUri != null)
                    return (RunReturn) (Integer) cr.update(mContentUri, values, getWhere(), getWhereArgs());
                else
                    throw new IllegalStateException("Database helper was null.");
            }
            case DELETE: {
                if (mDatabase != null) {
                    RunReturn value = (RunReturn) (Integer) mDatabase.delete(getWhere(), getWhereArgs());
                    close();
                    return value;
                } else if (mContentUri != null)
                    return (RunReturn) (Integer) cr.delete(mContentUri, getWhere(), getWhereArgs());
                else
                    throw new IllegalStateException("Database helper was null.");
            }
        }
        return null;
    }

    public void run(@NonNull final RunCallback<RunReturn> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final RunReturn changed = Query.this.run();
                if (mInquiry.mHandler == null) return;
                mInquiry.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.result(changed);
                    }
                });
            }
        }).start();
    }

    public void close() {
        if (mDatabase != null) mDatabase.close();
    }
}