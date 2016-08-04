package com.afollestad.inquiry;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Inquiry {

    private static HashMap<String, Inquiry> mInstances;

    private static String getInstanceName(@NonNull Context forContext) {
        return forContext.getClass().getName();
    }

    private static void LOG(@NonNull String msg, @Nullable Object... args) {
        if (args != null)
            msg = String.format(msg, args);
        Log.d("Inquiry", msg);
    }

    protected Context mContext;
    protected Handler mHandler;
    @Nullable
    protected String mDatabaseName;
    protected int mDatabaseVersion = 1;
    protected String mInstanceName;

    private Inquiry(@NonNull Context context) {
        //noinspection ConstantConditions
        if (context == null)
            throw new IllegalArgumentException("Context can't be null.");
        mContext = context;
        mInstanceName = getInstanceName(context);
        mDatabaseVersion = 1;
    }

    public static class Builder {

        private Inquiry newInstance;
        private boolean used;

        public Builder(@NonNull Context context, @Nullable String databaseName) {
            newInstance = new Inquiry(context);
            if (databaseName == null || databaseName.trim().isEmpty()) {
                databaseName = "default_db";
                LOG("Using default database name: %s", databaseName);
            }
            newInstance.mDatabaseName = databaseName;
        }

        @NonNull
        public Builder instanceName(@Nullable String name) {
            newInstance.mInstanceName = name;
            return this;
        }

        @NonNull
        public Builder databaseVersion(@IntRange(from = 1, to = Integer.MAX_VALUE) int version) {
            newInstance.mDatabaseVersion = version;
            return this;
        }

        @NonNull
        public Builder handler(@Nullable Handler handler) {
            newInstance.mHandler = handler;
            return this;
        }

        @NonNull
        public Inquiry build() {
            return build(true);
        }

        @NonNull
        public Inquiry build(boolean persist) {
            final String name = newInstance.mInstanceName;
            if (used)
                throw new IllegalStateException("This Builder was already used to build instance " + name);
            this.used = true;

            if (persist) {
                if (mInstances == null)
                    mInstances = new HashMap<>();
                else if (mInstances.containsKey(name))
                    mInstances.get(name).destroyInstance();
                mInstances.put(name, newInstance);
            }

            if (newInstance.mHandler == null)
                newInstance.mHandler = new Handler();
            LOG("Built instance %s", name);

            return newInstance;
        }
    }

    @NonNull
    public static Inquiry copy(@NonNull Inquiry instance, @NonNull String newInstanceName, boolean persist) {
        return new Inquiry.Builder(instance.mContext, instance.mDatabaseName)
                .handler(instance.mHandler)
                .databaseVersion(instance.mDatabaseVersion)
                .instanceName(newInstanceName)
                .build();
    }

    @NonNull
    public static Inquiry copy(@NonNull Inquiry instance, @NonNull Context newContext, boolean persist) {
        return copy(instance, getInstanceName(newContext), persist);
    }

    public void destroyInstance() {
        if (mInstanceName != null) {
            mInstances.remove(mInstanceName);
            mInstanceName = null;
        }
        mContext = null;
        mHandler = null;
        mDatabaseName = null;
        mDatabaseVersion = 0;
    }

    public static void destroy(Context context) {
        destroy(getInstanceName(context));
    }

    public static void destroy(String instanceName) {
        if (!mInstances.containsKey(instanceName)) {
            LOG("No instances found to destroy by name %s.", instanceName);
            return;
        }

        final Inquiry instance = mInstances.get(instanceName);
        instance.destroyInstance();
        mInstances.remove(instanceName);
    }

    public void dropTable(@NonNull String tableName) {
        final SQLiteDatabase db = new SQLiteHelper(mContext, mDatabaseName, null, null, mDatabaseVersion).getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        db.close();
    }

    @NonNull
    public static Inquiry get(@NonNull Context context) {
        return get(getInstanceName(context));
    }

    @NonNull
    public static Inquiry get(@NonNull String instanceName) {
        if (!mInstances.containsKey(instanceName))
            throw new IllegalStateException(String.format("No persisted instance found for %s, or it's been garbage collected.", instanceName));
        return mInstances.get(instanceName);
    }

    @NonNull
    public <RowType> Query<RowType, Integer> selectFrom(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.SELECT, rowType, mDatabaseVersion);
    }

    @NonNull
    public <RowType> Query<RowType, Integer> selectFrom(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.SELECT, rowType);
    }

    @NonNull
    public <RowType> Query<RowType, Long[]> insertInto(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.INSERT, rowType, mDatabaseVersion);
    }

    @NonNull
    public <RowType> Query<RowType, Long[]> insertInto(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.INSERT, rowType);
    }

    @NonNull
    public <RowType> Query<RowType, Integer> update(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.UPDATE, rowType, mDatabaseVersion);
    }

    @NonNull
    public <RowType> Query<RowType, Integer> update(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.UPDATE, rowType);
    }

    @NonNull
    public <RowType> Query<RowType, Integer> deleteFrom(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.DELETE, rowType, mDatabaseVersion);
    }

    @NonNull
    public <RowType> Query<RowType, Integer> deleteFrom(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.DELETE, rowType);
    }
}