package com.afollestad.inquiry;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Inquiry {

    private static Inquiry mInquiry;

    protected Context mContext;
    protected Handler mHandler;
    @Nullable
    protected String mDatabaseName;
    protected int mDatabaseVersion = 1;

    private Inquiry() {
        mHandler = new Handler();
    }

    @NonNull
    public static Inquiry init(@NonNull Context context, @Nullable String databaseName,
                               @IntRange(from = 1, to = Integer.MAX_VALUE) int databaseVersion) {
        //noinspection ConstantConditions
        if (context == null)
            throw new IllegalArgumentException("Context can't be null.");
        if (mInquiry == null)
            mInquiry = new Inquiry();
        mInquiry.mContext = context;
        mInquiry.mDatabaseName = databaseName;
        mInquiry.mDatabaseVersion = databaseVersion;
        return mInquiry;
    }

    @NonNull
    public static Inquiry init(@NonNull Context context) {
        return init(context, null, 1);
    }

    public static void deinit() {
        if (mInquiry != null) {
            mInquiry.mContext = null;
            mInquiry.mHandler = null;
            mInquiry.mDatabaseName = null;
            mInquiry.mDatabaseVersion = 0;
            mInquiry = null;
        }
    }

    public void dropTable(@NonNull String tableName) {
        new SQLiteHelper(mContext, mDatabaseName, null, null, mDatabaseVersion)
                .getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + tableName);
    }

    @NonNull
    public static Inquiry get() {
        if (mInquiry == null)
            throw new IllegalStateException("Inquiry not initialized, or has been garbage collected.");
        return mInquiry;
    }

    @NonNull
    public <RowType> Query<RowType> selectFrom(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.SELECT, rowType, mDatabaseVersion);
    }

    @NonNull
    public <RowType> Query<RowType> selectFrom(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.SELECT, rowType);
    }

    @NonNull
    public <RowType> Query<RowType> insertInto(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.INSERT, rowType, mDatabaseVersion);
    }

    @NonNull
    public <RowType> Query<RowType> insertInto(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.INSERT, rowType);
    }

    @NonNull
    public <RowType> Query<RowType> update(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.UPDATE, rowType, mDatabaseVersion);
    }

    @NonNull
    public <RowType> Query<RowType> update(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.UPDATE, rowType);
    }

    @NonNull
    public <RowType> Query<RowType> deleteFrom(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.DELETE, rowType, mDatabaseVersion);
    }

    @NonNull
    public <RowType> Query<RowType> deleteFrom(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.DELETE, rowType);
    }
}