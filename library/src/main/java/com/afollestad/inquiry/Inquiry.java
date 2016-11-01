package com.afollestad.inquiry;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.CheckResult;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.afollestad.inquiry.annotations.Column;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import static com.afollestad.inquiry.ClassRowConverter.getAllFields;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess")
public final class Inquiry {

    private static HashMap<String, Inquiry> mInstances;
    private HashMap<String, Field> mIdFieldCache;

    private static String getInstanceName(@NonNull Context forContext) {
        return forContext.getClass().getName();
    }

    private static void LOG(@NonNull String msg, @Nullable Object... args) {
        if (args != null)
            msg = String.format(msg, args);
        Log.d("Inquiry", msg);
    }

    Context mContext;
    Handler mHandler;
    @Nullable
    String mDatabaseName;
    boolean mCacheIdFields;
    private int mDatabaseVersion = 1;
    private String mInstanceName;

    private SQLiteHelper mDatabase;

    public SQLiteHelper getDatabase() {
        if (mDatabase == null && mDatabaseName != null) {
            mDatabase = new SQLiteHelper(mContext, mDatabaseName, mDatabaseVersion);
        }
        return mDatabase;
    }

    private Inquiry(@NonNull Context context) {
        //noinspection ConstantConditions
        if (context == null)
            throw new IllegalArgumentException("Context can't be null.");
        mContext = context;
        mInstanceName = getInstanceName(context);
        mDatabaseVersion = 1;
        mIdFieldCache = new HashMap<>();
    }

    @Nullable
    Field getIdField(Class<?> forClass) {
        Field idField = null;
        if (mCacheIdFields && mIdFieldCache != null) {
            idField = mIdFieldCache.get(forClass.getName());
            if (idField != null) return idField;
        }
        List<Field> allFields = getAllFields(forClass);
        for (Field field : allFields) {
            final Column colAnn = field.getAnnotation(Column.class);
            if (colAnn == null) continue;
            final String colName = ClassRowConverter.selectColumnName(colAnn, field);
            if (colName.equals("_id")) {
                if (!colAnn.autoIncrement() || !colAnn.primaryKey())
                    throw new IllegalStateException("Fields which represent _id columns MUST have autoIncrement() AND primaryKey() enabled.");
                if (field.getType() != Long.class && field.getType() != long.class)
                    throw new IllegalStateException("Fields which represent _id columns MUST be of type Long.");
                idField = field;
            }
        }
        if (idField == null)
            return null;
        if (mCacheIdFields) {
            if (mIdFieldCache == null)
                mIdFieldCache = new HashMap<>();
            mIdFieldCache.put(forClass.getName(), idField);
        }
        return idField;
    }

    public static class Builder {

        private Inquiry newInstance;
        private boolean used;

        protected Builder(@NonNull Context context, @Nullable String databaseName) {
            newInstance = new Inquiry(context);
            if (databaseName == null || databaseName.trim().isEmpty()) {
                databaseName = "default_db";
                LOG("Using default database name: %s", databaseName);
            }
            newInstance.mDatabaseName = databaseName;
            newInstance.mCacheIdFields = true;
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
        public Builder cacheIdFields(boolean cache) {
            newInstance.mCacheIdFields = cache;
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

    public static Inquiry.Builder newInstance(@NonNull Context context, @Nullable String databaseName) {
        return new Inquiry.Builder(context, databaseName);
    }

    @CheckResult
    @NonNull
    public static Inquiry copy(@NonNull Inquiry instance, @NonNull String newInstanceName, boolean persist) {
        return new Inquiry.Builder(instance.mContext, instance.mDatabaseName)
                .handler(instance.mHandler)
                .databaseVersion(instance.mDatabaseVersion)
                .instanceName(newInstanceName)
                .build(persist);
    }

    @CheckResult
    @NonNull
    public static Inquiry copy(@NonNull Inquiry instance, @NonNull Context newContext, boolean persist) {
        return copy(instance, getInstanceName(newContext), persist);
    }

    public boolean isDestroyed() {
        return mContext == null;
    }

    public void destroyInstance() {
        if (mDatabase != null) {
            mDatabase.close();
            mDatabase = null;
        }
        if (mIdFieldCache != null) {
            mIdFieldCache.clear();
            mIdFieldCache = null;
        }
        if (mInstanceName != null) {
            if (mInstances != null)
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
        if (mInstances == null || !mInstances.containsKey(instanceName)) {
            LOG("No instances found to destroy by name %s.", instanceName);
            return;
        }

        final Inquiry instance = mInstances.get(instanceName);
        instance.destroyInstance();
        mInstances.remove(instanceName);
    }

    public void dropTable(@NonNull Class<?> rowCls) {
        SQLiteDatabase db = new SQLiteHelper(mContext, mDatabaseName, mDatabaseVersion).getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + ClassRowConverter.getClassTable(rowCls));
        db.close();
    }

    @Deprecated
    public void dropTable(@NonNull String tableName) {
        SQLiteDatabase db = new SQLiteHelper(mContext, mDatabaseName, mDatabaseVersion).getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        db.close();
    }

    @CheckResult
    @NonNull
    public static Inquiry get(@NonNull Context context) {
        return get(getInstanceName(context));
    }

    @CheckResult
    @NonNull
    public static Inquiry get(@NonNull String instanceName) {
        if (mInstances == null || !mInstances.containsKey(instanceName))
            throw new IllegalStateException(String.format("No persisted instance found for %s, or it's been garbage collected.", instanceName));
        return mInstances.get(instanceName);
    }

    @CheckResult
    @NonNull
    @Deprecated
    public <RowType> Query<RowType, Integer> selectFrom(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.SELECT, rowType);
    }

    @CheckResult
    @NonNull
    public <RowType> Query<RowType, Integer> select(@NonNull Class<RowType> rowType) {
        return new Query<>(this, ClassRowConverter.getClassTable(rowType), Query.SELECT, rowType);
    }

    @CheckResult
    @NonNull
    public <RowType> Query<RowType, Integer> selectFrom(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.SELECT, rowType);
    }

    @CheckResult
    @NonNull
    @Deprecated
    public <RowType> Query<RowType, Long[]> insertInto(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.INSERT, rowType);
    }

    @CheckResult
    @NonNull
    public <RowType> Query<RowType, Long[]> insert(@NonNull Class<RowType> rowType) {
        return new Query<>(this, ClassRowConverter.getClassTable(rowType), Query.INSERT, rowType);
    }

    @CheckResult
    @NonNull
    public <RowType> Query<RowType, Long[]> insertInto(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.INSERT, rowType);
    }

    @CheckResult
    @NonNull
    @Deprecated
    public <RowType> Query<RowType, Integer> update(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.UPDATE, rowType);
    }

    @CheckResult
    @NonNull
    public <RowType> Query<RowType, Integer> update(@NonNull Class<RowType> rowType) {
        return new Query<>(this, ClassRowConverter.getClassTable(rowType), Query.UPDATE, rowType);
    }

    @CheckResult
    @NonNull
    public <RowType> Query<RowType, Integer> update(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.UPDATE, rowType);
    }

    @CheckResult
    @NonNull
    @Deprecated
    public <RowType> Query<RowType, Integer> deleteFrom(@NonNull String table, @NonNull Class<RowType> rowType) {
        return new Query<>(this, table, Query.DELETE, rowType);
    }

    @CheckResult
    @NonNull
    public <RowType> Query<RowType, Integer> delete(@NonNull Class<RowType> rowType) {
        return new Query<>(this, ClassRowConverter.getClassTable(rowType), Query.DELETE, rowType);
    }

    @CheckResult
    @NonNull
    public <RowType> Query<RowType, Integer> deleteFrom(@NonNull Uri contentProviderUri, @NonNull Class<RowType> rowType) {
        return new Query<>(this, contentProviderUri, Query.DELETE, rowType);
    }
}