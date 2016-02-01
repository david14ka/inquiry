package com.afollestad.inquiry;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.inquiry.callbacks.GetCallback;
import com.afollestad.inquiry.callbacks.RunCallback;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Query<RowType, RunReturn> {

    protected final static int SELECT = 1;
    protected final static int INSERT = 2;
    protected final static int UPDATE = 3;
    protected final static int DELETE = 4;

    private final Inquiry mInquiry;
    private Uri mContentUri;
    private final int mQueryType;
    @Nullable
    private final Class<RowType> mRowClass;
    @Nullable
    private SQLiteHelper mDatabase;

    protected Query(@NonNull Inquiry inquiry, @NonNull Uri contentUri, int type, @Nullable Class<RowType> mClass) {
        mInquiry = inquiry;
        mContentUri = contentUri;
        if (mContentUri.getScheme() == null || !mContentUri.getScheme().equals("content"))
            throw new IllegalStateException("You can only use content:// URIs for content providers.");
        mQueryType = type;
        mRowClass = mClass;
    }

    protected Query(@NonNull Inquiry inquiry, @NonNull String tableName, int type, @Nullable Class<RowType> mClass, int databaseVersion) {
        mInquiry = inquiry;
        mQueryType = type;
        mRowClass = mClass;
        if (inquiry.mDatabaseName == null)
            throw new IllegalStateException("Inquiry was not initialized with a database name, it can only use content providers in this configuration.");
        mDatabase = new SQLiteHelper(inquiry.mContext, inquiry.mDatabaseName,
                tableName, ClassRowConverter.getClassSchema(mClass), databaseVersion);
    }

    private String[] mOnlyUpdate;
    private String mSelection;
    private String[] mSelectionArgs;
    private String mSortOrder;
    private int mLimit;
    private RowType[] mValues;

    public Query<RowType, RunReturn> atPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int position) {
        Cursor cursor;
        if (mContentUri != null) {
            cursor = mInquiry.mContext.getContentResolver().query(mContentUri, null, mSelection, mSelectionArgs, null);
        } else {
            if (mDatabase == null) throw new IllegalStateException("Database helper was null.");
            cursor = mDatabase.query(null, mSelection, mSelectionArgs, null);
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
            mSelection = "_id = ?";
            mSelectionArgs = new String[]{Integer.toString(idValue)};
            cursor.close();
        }
        return this;
    }

    public Query<RowType, RunReturn> where(@NonNull String selection, @Nullable Object... selectionArgs) {
        mSelection = selection;
        if (selectionArgs != null) {
            mSelectionArgs = new String[selectionArgs.length];
            for (int i = 0; i < selectionArgs.length; i++)
                mSelectionArgs[i] = (selectionArgs[i] + "");
        } else {
            mSelectionArgs = null;
        }
        return this;
    }

    public Query<RowType, RunReturn> sort(@NonNull String sortOrder) {
        mSortOrder = sortOrder;
        return this;
    }

    public Query<RowType, RunReturn> limit(int limit) {
        mLimit = limit;
        return this;
    }

    @SuppressWarnings("unchecked")
    protected final Query<RowType, RunReturn> value(@NonNull Object value) {
        mValues = (RowType[]) Array.newInstance(mRowClass, 1);
        Array.set(mValues, 0, value);
        return this;
    }

    @SafeVarargs
    public final Query<RowType, RunReturn> values(@NonNull RowType... values) {
        mValues = values;
        return this;
    }

    public Query<RowType, RunReturn> onlyUpdate(@NonNull String... values) {
        mOnlyUpdate = values;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private RowType[] getInternal(int limit) {
        if (mRowClass == null) return null;
        else if (mInquiry.mContext == null)
            throw new IllegalStateException("Inquiry's context was null. Deinit() was probably run already.");
        final String[] projection = ClassRowConverter.generateProjection(mRowClass);
        if (mQueryType == SELECT) {
            String sort = mSortOrder;
            if (limit > -1) sort += String.format(" LIMIT %d", limit);
            Cursor cursor;
            if (mContentUri != null) {
                cursor = mInquiry.mContext.getContentResolver().query(mContentUri, projection, mSelection, mSelectionArgs, sort);
            } else {
                if (mDatabase == null) throw new IllegalStateException("Database helper was null.");
                cursor = mDatabase.query(projection, mSelection, mSelectionArgs, sort);
            }
            if (cursor != null) {
                RowType[] results = null;
                if (cursor.getCount() > 0) {
                    results = (RowType[]) Array.newInstance(mRowClass, cursor.getCount());
                    int index = 0;
                    while (cursor.moveToNext()) {
                        results[index] = ClassRowConverter.cursorToCls(cursor, mRowClass);
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
    public RowType one() {
        if (mRowClass == null) return null;
        RowType[] results = getInternal(1);
        if (results == null || results.length == 0)
            return null;
        return results[0];
    }

    @Nullable
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

    @SuppressWarnings("unchecked")
    public RunReturn run() {
        if (mQueryType != DELETE && (mValues == null || mValues.length == 0))
            throw new IllegalStateException("No values were provided for this query to run.");
        else if (mInquiry.mContext == null)
            throw new IllegalStateException("Inquiry's context was null. Deinit() was probably run already.");
        final ContentResolver cr = mInquiry.mContext.getContentResolver();
        final List<Field> clsFields = ClassRowConverter.getAllFields(mRowClass);
        switch (mQueryType) {
            case INSERT:
                final Field idField = ClassRowConverter.getIdField(clsFields);
                Long[] insertedIds = new Long[mValues.length];
                if (mDatabase != null) {
                    for (int i = 0; i < mValues.length; i++) {
                        final RowType row = mValues[i];
                        insertedIds[i] = mDatabase.insert(ClassRowConverter.clsToVals(row, null, clsFields));
                        ClassRowConverter.setIdField(row, idField, insertedIds[i]);
                    }
                } else if (mContentUri != null) {
                    for (int i = 0; i < mValues.length; i++) {
                        final RowType row = mValues[i];
                        final Uri uri = cr.insert(mContentUri, ClassRowConverter.clsToVals(row, null, clsFields));
                        if (uri == null) return (RunReturn) (Long) (-1L);
                        insertedIds[i] = Long.parseLong(uri.getLastPathSegment());
                        ClassRowConverter.setIdField(row, idField, insertedIds[i]);
                    }
                } else
                    throw new IllegalStateException("Database helper was null.");
                close();
                return (RunReturn) insertedIds;
            case UPDATE: {
                final ContentValues values = ClassRowConverter.clsToVals(mValues[mValues.length - 1], mOnlyUpdate, clsFields);
                if (mDatabase != null) {
                    RunReturn value = (RunReturn) (Integer) mDatabase.update(values, mSelection, mSelectionArgs);
                    close();
                    return value;
                } else if (mContentUri != null)
                    return (RunReturn) (Integer) cr.update(mContentUri, values, mSelection, mSelectionArgs);
                else
                    throw new IllegalStateException("Database helper was null.");
            }
            case DELETE: {
                if (mDatabase != null) {
                    RunReturn value = (RunReturn) (Integer) mDatabase.delete(mSelection, mSelectionArgs);
                    close();
                    return value;
                } else if (mContentUri != null)
                    return (RunReturn) (Integer) cr.delete(mContentUri, mSelection, mSelectionArgs);
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