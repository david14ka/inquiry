package com.afollestad.inquiry;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.ForeignKey;
import com.afollestad.inquiry.annotations.Table;
import com.afollestad.inquiry.lazyloading.LazyLoaderList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.afollestad.inquiry.DataType.BLOB;
import static com.afollestad.inquiry.DataType.INTEGER;
import static com.afollestad.inquiry.DataType.REAL;
import static com.afollestad.inquiry.DataType.TEXT;
import static com.afollestad.inquiry.DataType.UNKNOWN;

/**
 * @author Aidan Follestad (afollestad)
 */
class Converter {

    static String getClassTableName(Class<?> cls) {
        Table tableAnn = cls.getAnnotation(Table.class);
        if (tableAnn == null)
            throw new IllegalArgumentException("Class " + cls.getName() + " does not use the @Table annotation.");
        String name = tableAnn.name();
        if (name.trim().isEmpty())
            name = cls.getSimpleName().toLowerCase() + "s";
        return name;
    }

    static String getClassSchema(Class<?> cls) {
        StringBuilder sb = new StringBuilder();
        List<FieldDelegate> proxyList = classFieldDelegates(cls);

        for (FieldDelegate proxy : proxyList) {
            final String schema = proxy.schema();
            if (schema == null) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(schema);
        }

        if (sb.length() == 0)
            throw new IllegalStateException("Class " + cls.getName() + " has no @Column fields.");
        Log.d("Inquiry", String.format("Scheme for %s: %s", cls.getName(), sb.toString()));
        return sb.toString();
    }

    @DataType.TypeDef private static int cursorTypeToColumnType(int cursorType) {
        switch (cursorType) {
            default:
                return BLOB;
            case Cursor.FIELD_TYPE_FLOAT:
                return REAL;
            case Cursor.FIELD_TYPE_INTEGER:
                return INTEGER;
            case Cursor.FIELD_TYPE_STRING:
                return TEXT;
        }
    }

    /**
     * @param inquiry           An Inquiry instance
     * @param tableName         The name of the child table
     * @param foreignColumnName The name of the column in the child which hooks it ot the parent
     * @param inverseFieldName  The optional name of a field in the child class which gets set to the parent
     * @param row               The DB row
     * @param fieldType         The type of field in the parent class, used to check the return type of this method
     * @param childType         The type of the child class
     * @return object, array, or list depending on the fieldType
     */
    private static Object processForeignKey(@NonNull Inquiry inquiry, @NonNull String tableName,
                                            @NonNull String foreignColumnName, @Nullable String inverseFieldName,
                                            @NonNull Object row, @Nullable Class<?> fieldType, @NonNull Class<?> childType) {
        if (fieldType == null) {
            fieldType = List.class;
        }

        final FieldDelegate idProxy = inquiry.getIdDelegate(row.getClass());
        if (idProxy == null) {
            throw new IllegalStateException("You cannot use the @ForeignKey annotation " +
                    "on a field within a class that doesn't have an _id column.");
        }
        final Inquiry fkInstance = Inquiry.copy(inquiry, "[@fk]:" + tableName + "//" + foreignColumnName, false);

        Long rowId = idProxy.get(row);
        Object[] valuesArray = fkInstance
                .selectFrom(tableName, childType)
                .where(foreignColumnName + " = ?", rowId)
                .all();

        if (valuesArray == null || valuesArray.length == 0) {
            if (Utils.classImplementsList(fieldType))
                return new ArrayList(0);
            else return null;
        } else {
            if (inverseFieldName != null && !inverseFieldName.isEmpty()) {
                for (Object val : valuesArray) {
                    FieldDelegate inverseProxy = getProxyByName(
                            classFieldDelegates(childType), inverseFieldName);
                    if (inverseProxy == null) {
                        throw new IllegalStateException("Inverse field " +
                                inverseFieldName + " not found in " + childType);
                    }
                    try {
                        inverseProxy.set(val, row);
                    } catch (Throwable t) {
                        Utils.wrapInReIfNecessary(t);
                        return null;
                    }
                }
            }

            fkInstance.destroyInstance();
            if (Utils.classImplementsList(fieldType)) {
                List list = new ArrayList(valuesArray.length);
                Collections.addAll(list, valuesArray);
                return list;
            } else if (fieldType.isArray()) {
                return valuesArray;
            } else {
                return valuesArray[0];
            }
        }
    }

    private static void loadFieldIntoRow(Query query, Cursor cursor, FieldDelegate proxy,
                                         Object row, int columnIndex,
                                         @DataType.TypeDef int columnType) throws Exception {
        Class<?> fieldType = proxy.getType();
        ForeignKey fkAnn = proxy.getForeignKey();
        if (fkAnn != null) {
            Class<?> childType = Utils.getGenericTypeOfProxy(proxy);

            if (Utils.classExtendsLazyLoader(fieldType)) {
                @SuppressWarnings("unchecked")
                LazyLoaderList loader = new LazyLoaderList(query.getInquiryInstance(),
                        fkAnn.tableName(), fkAnn.foreignColumnName(),
                        fkAnn.inverseFieldName(), row, childType) {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void _performLazyLoad() {
                        this.items = (List) processForeignKey(this.inquiry, this.tableName, this.foreignColumnName,
                                this.inverseFieldName, this.row, null, this.childType);
                    }
                };
                proxy.set(row, loader);
                return;
            }

            Object value = processForeignKey(query.getInquiryInstance(), fkAnn.tableName(),
                    fkAnn.foreignColumnName(), fkAnn.inverseFieldName(), row, fieldType, childType);
            proxy.set(row, value);
            return;
        }

        if (cursor.isNull(columnIndex)) {
            if (fieldType == short.class || fieldType == Short.class ||
                    fieldType == int.class || fieldType == Integer.class ||
                    fieldType == long.class || fieldType == Long.class) {
                proxy.set(row, 0);
            } else if (fieldType == float.class || fieldType == Float.class) {
                proxy.set(row, 0f);
            } else if (fieldType == double.class || fieldType == Double.class) {
                proxy.set(row, 0d);
            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                proxy.set(row, false);
            } else {
                proxy.set(row, null);
            }
            return;
        }

        final String columnName = proxy.name();
        switch (columnType) {
            case BLOB:
                byte[] blob = cursor.getBlob(columnIndex);
                if (blob == null)
                    proxy.set(row, null);
                else if (fieldType == byte.class || fieldType == Byte.class)
                    proxy.set(row, blob[0]);
                else if (fieldType == byte[].class || fieldType == Byte[].class)
                    proxy.set(row, blob);
                else if (fieldType == Bitmap.class)
                    proxy.set(row, BitmapFactory.decodeByteArray(blob, 0, blob.length));
                else
                    proxy.set(row, deserializeObject(blob, fieldType));
                break;
            case REAL:
                if (fieldType == short.class || fieldType == Short.class)
                    proxy.set(row, (short) cursor.getFloat(columnIndex));
                else if (fieldType == int.class || fieldType == Integer.class)
                    proxy.set(row, (int) cursor.getFloat(columnIndex));
                else if (fieldType == long.class || fieldType == Long.class)
                    proxy.set(row, (long) cursor.getFloat(columnIndex));
                else if (fieldType == float.class || fieldType == Float.class)
                    proxy.set(row, cursor.getFloat(columnIndex));
                else if (fieldType == double.class || fieldType == Double.class)
                    proxy.set(row, cursor.getDouble(columnIndex));
                else
                    throw new IllegalStateException(String.format("Column %s of type REAL " +
                                    "(float/double) doesn't match field/method of type %s",
                            columnName, fieldType.getName()));
                break;
            case INTEGER:
                if (fieldType == short.class || fieldType == Short.class)
                    proxy.set(row, cursor.getShort(columnIndex));
                else if (fieldType == int.class || fieldType == Integer.class)
                    proxy.set(row, cursor.getInt(columnIndex));
                else if (fieldType == long.class || fieldType == Long.class)
                    proxy.set(row, cursor.getLong(columnIndex));
                else if (fieldType == boolean.class || fieldType == Boolean.class)
                    proxy.set(row, cursor.getInt(columnIndex) == 1);
                else if (fieldType == float.class || fieldType == Float.class)
                    proxy.set(row, (float) cursor.getInt(columnIndex));
                else if (fieldType == double.class || fieldType == Double.class)
                    proxy.set(row, (double) cursor.getInt(columnIndex));
                else
                    throw new IllegalStateException(String.format("Column %s of type INTEGER " +
                                    "(decimal) doesn't match field/method of type %s",
                            columnName, fieldType.getName()));
                break;
            case TEXT:
                String text = cursor.getString(columnIndex);
                if (fieldType == String.class || fieldType == CharSequence.class)
                    proxy.set(row, text);
                else if (fieldType == char[].class || fieldType == Character[].class)
                    proxy.set(row, text != null && text.length() > 0 ? text.toCharArray() : null);
                else if (fieldType == char.class || fieldType == Character.class)
                    proxy.set(row, text != null && text.length() > 0 ? text.charAt(0) : null);
                else
                    throw new IllegalStateException(String.format("Column %s of type TEXT " +
                                    "(string) doesn't match field/method of type %s",
                            columnName, fieldType.getName()));
                break;
        }
    }

    private static HashMap<String, FieldDelegate> buildProxyCache(@NonNull Class<?> cls,
                                                                  @Nullable Class<?> builderCls,
                                                                  @NonNull List<FieldDelegate> outForeignKeys) {
        final HashMap<String, FieldDelegate> cacheMap = new HashMap<>();
        final List<FieldDelegate> proxiesList = classFieldDelegates(cls, false, builderCls);

        for (FieldDelegate proxy : proxiesList) {
            ForeignKey fkAnn = proxy.getForeignKey();
            if (fkAnn != null) {
                outForeignKeys.add(proxy);
                continue;
            }
            cacheMap.put(proxy.name(), proxy);
        }

        return cacheMap;
    }

    static <T> T cursorToObject(Query query, Cursor cursor, Class<T> cls) {
        T resultObject = null;
        Object rowBuilder = null;
        Object objectToActOn;

        final Class<?> builderCls = query.getInquiryInstance().getBuilderClass(cls);
        if (builderCls != null) {
            rowBuilder = Utils.newInstance(query.getInquiryInstance(), builderCls);
            objectToActOn = rowBuilder;
        } else {
            resultObject = Utils.newInstance(query.getInquiryInstance(), cls);
            objectToActOn = resultObject;
        }

        final List<FieldDelegate> foreignKeyList = new ArrayList<>(0);
        final HashMap<String, FieldDelegate> cacheMap = buildProxyCache(cls, builderCls, foreignKeyList);

        int columnIndex;
        for (columnIndex = 0; columnIndex < cursor.getColumnCount(); columnIndex++) {
            String columnName = cursor.getColumnName(columnIndex);
            if (columnName == null)
                throw new IllegalStateException("Cursor returned null for the columnName at index " + columnIndex);
            int columnType = cursorTypeToColumnType(cursor.getType(columnIndex));
            try {
                final FieldDelegate proxy = cacheMap.get(columnName);
                if (proxy == null) {
                    throw new IllegalStateException("Unable to map database column " + columnName +
                            " to field/method in " + cls.getName());
                }
                loadFieldIntoRow(query, cursor, proxy, objectToActOn, columnIndex, columnType);
            } catch (Exception e) {
                Utils.wrapInReIfNecessary(e);
            }
        }

        if (foreignKeyList.size() > 0) {
            for (FieldDelegate proxy : foreignKeyList) {
                try {
                    loadFieldIntoRow(query, cursor, proxy, objectToActOn, columnIndex, UNKNOWN);
                } catch (Throwable t) {
                    Utils.wrapInReIfNecessary(t);
                }
            }
        }

        if (rowBuilder != null) {
            Method buildMethod = query.getInquiryInstance().getBuildMethod(cls, builderCls);
            if (buildMethod == null) {
                throw new IllegalStateException("Builder " + builderCls.getName() + " must " +
                        "contain a void method called build() which returns " + cls.getName());
            }
            try {
                //noinspection unchecked
                return (T) buildMethod.invoke(rowBuilder);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to invoke build() of " +
                        builderCls.getName(), e);
            }
        } else {
            return resultObject;
        }
    }

    @Nullable static FieldDelegate getProxyByName(@Nullable List<FieldDelegate> proxyList,
                                                  @NonNull String name,
                                                  @Nullable Class<?>... requiredTypes) {
        if (proxyList == null || proxyList.size() == 0) return null;
        for (FieldDelegate proxy : proxyList) {
            if (requiredTypes != null && requiredTypes.length > 0) {
                boolean matchesOneRequiredType = false;
                for (Class<?> cls : requiredTypes) {
                    if (proxy.getType() == cls) {
                        matchesOneRequiredType = true;
                        break;
                    }
                }
                if (!matchesOneRequiredType) continue;
            }
            if (proxy.name().equals(name)) return proxy;
        }
        return null;
    }

    static String[] generateProjection(@NonNull Class<?> cls) {
        ArrayList<String> projectionList = new ArrayList<>();
        List<FieldDelegate> proxyList = classFieldDelegates(cls);
        for (FieldDelegate proxy : proxyList) {
            if (proxy.isForeignKey() || proxy.ignore()) continue;
            projectionList.add(proxy.name());
        }
        return projectionList.toArray(new String[projectionList.size()]);
    }

    static List<FieldDelegate> classFieldDelegates(@NonNull Class<?> cls) {
        return classFieldDelegates(cls, false);
    }

    static List<FieldDelegate> classFieldDelegates(@NonNull Class<?> cls,
                                                   boolean methodsOnly) {
        return classFieldDelegates(cls, methodsOnly, null);
    }

    static List<FieldDelegate> classFieldDelegates(@NonNull Class<?> cls,
                                                   boolean methodsOnly,
                                                   @Nullable Class<?> builderCls) {
        final List<FieldDelegate> proxiesList = new ArrayList<>(8);
        if (builderCls != null) {
            for (Method method : builderCls.getDeclaredMethods()) {
                if (method.getParameterTypes() == null ||
                        method.getParameterTypes().length == 0) {
                    // Ignore methods without parameters
                    continue;
                }
                FieldDelegate proxy = new FieldDelegate(cls, builderCls, method);
                if (proxy.ignore()) continue;
                proxiesList.add(proxy);
            }
        } else {
            while (true) {
                if (!methodsOnly) {
                    for (Field field : cls.getDeclaredFields()) {
                        FieldDelegate proxy = new FieldDelegate(field, null, cls);
                        if (proxy.ignore()) continue;
                        proxiesList.add(proxy);
                    }
                }
                for (Method method : cls.getDeclaredMethods()) {
                    if (method.getParameterTypes() != null && method.getParameterTypes().length > 0) {
                        // Ignore setter methods at this point
                        continue;
                    }
                    FieldDelegate proxy = new FieldDelegate(null, method, cls);
                    if (proxy.ignore()) continue;
                    proxiesList.add(proxy);
                }
                cls = cls.getSuperclass();
                if (cls == null) {
                    break;
                }
            }
        }
        return proxiesList;
    }

    @Nullable static RowValues classToValues(@NonNull Object row,
                                             @Nullable String[] projectionArray,
                                             @NonNull List<FieldDelegate> proxiesList,
                                             @NonNull Map<Object, FieldDelegate> foreignChildrenMap) {
        try {
            RowValues resultValues = new RowValues();
            int columnCount = 0;
            for (FieldDelegate proxy : proxiesList) {
                if (projectionArray != null && projectionArray.length > 0) {
                    boolean skip = true;
                    for (String projectionValue : projectionArray) {
                        if (projectionValue != null &&
                                projectionValue.equalsIgnoreCase(proxy.name())) {
                            skip = false;
                            break;
                        }
                    }
                    if (skip) continue;
                }

                if (proxy.isForeignKey()) {
                    foreignChildrenMap.put(row, proxy);
                    continue;
                }

                Column columnAnnotation = proxy.getColumn();
                if (columnAnnotation == null) continue;

                columnCount++;
                Class<?> fldType = proxy.getType();
                Object fldVal = proxy.get(row);

                if (fldVal == null) continue;
                if (columnAnnotation.autoIncrement() && (Long) fldVal <= 0) continue;

                final String columnName = proxy.name();
                if (fldType.equals(String.class)) {
                    resultValues.put(columnName, (String) fldVal);
                } else if (fldType.equals(char[].class) || fldType.equals(Character[].class)) {
                    resultValues.put(columnName, new String((char[]) fldVal));
                } else if (fldType.equals(Float.class) || fldType.equals(float.class)) {
                    resultValues.put(columnName, (float) fldVal);
                } else if (fldType.equals(Double.class) || fldType.equals(double.class)) {
                    resultValues.put(columnName, (double) fldVal);
                } else if (fldType.equals(Short.class) || fldType.equals(short.class)) {
                    resultValues.put(columnName, (short) fldVal);
                } else if (fldType.equals(Integer.class) || fldType.equals(int.class)) {
                    resultValues.put(columnName, (int) fldVal);
                } else if (fldType.equals(Long.class) || fldType.equals(long.class)) {
                    resultValues.put(columnName, (long) fldVal);
                } else if (fldType.equals(char.class) || fldType.equals(Character.class)) {
                    resultValues.put(columnName, Character.toString((char) fldVal));
                } else if (fldType.equals(Boolean.class) || fldType.equals(boolean.class)) {
                    resultValues.put(columnName, ((boolean) fldVal) ? 1 : 0);
                } else if (fldType.equals(Bitmap.class)) {
                    resultValues.put(columnName, bitmapToBytes((Bitmap) fldVal));
                } else if (fldType.equals(Byte.class) || fldType.equals(byte.class)) {
                    resultValues.put(columnName, (byte) fldVal);
                } else if (fldType.equals(Byte[].class) || fldType.equals(byte[].class)) {
                    resultValues.put(columnName, (byte[]) fldVal);
                } else if (fldVal instanceof Serializable) {
                    resultValues.put(columnName, serializeObject(fldVal));
                } else {
                    throw new IllegalStateException(String.format("Class %s should be marked as " +
                            "Serializable, or field/method %s should use the @ForeignKey " +
                            "annotation instead of @Column.", fldType.getName(), proxy.originalName()));
                }
            }

            if (columnCount == 0) {
                throw new IllegalStateException("Class " + row.getClass().getName() + " " +
                        "has no column annotated fields/methods.");
            }
            return resultValues;
        } catch (Throwable t) {
            Utils.wrapInReIfNecessary(t);
            return null;
        }
    }

    private static byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        } finally {
            Utils.closeQuietely(stream);
        }
    }

    private static byte[] serializeObject(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize object of type " + obj.getClass().getName(), e);
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException ignored) {
            }
            try {
                bos.close();
            } catch (IOException ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserializeObject(byte[] data, Class<T> cls) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            return (T) in.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to deserialize data to type " + cls.getName(), e);
        } finally {
            Utils.closeQuietely(bis);
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}