package com.afollestad.inquiry;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.ForeignKey;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author Aidan Follestad (afollestad)
 */
final class FieldDelegate {

    @Nullable private Field field;
    @Nullable private Method getterMethod;
    @Nullable private Method setterMethod;

    // We're acting on a Builder class
    FieldDelegate(@NonNull Class<?> parentCls,
                  @NonNull Class<?> builderCls,
                  @NonNull Method setterMethod) {
        if (setterMethod.getReturnType() != builderCls) {
            throw new IllegalStateException("Builder setter methods must return the Builder instance.");
        }
        if (setterMethod.getParameterTypes() == null || setterMethod.getParameterTypes().length != 1) {
            throw new IllegalStateException("Builder setter methods must only have 1 parameter.");
        }
        this.setterMethod = setterMethod;

        String targetGetterName = setterMethod.getName();
        if (targetGetterName.startsWith("set"))
            targetGetterName = targetGetterName.substring(3);
        final String expectedSignature = setterMethod.getParameterTypes()[0].getName() +
                " " + targetGetterName + "()";

        try {
            this.getterMethod = parentCls.getDeclaredMethod(targetGetterName);
            if (this.getterMethod.getReturnType() != setterMethod.getParameterTypes()[0]) {
                throw new IllegalStateException("Getter " + getterMethod.getName() + "() must return " +
                        setterMethod.getParameterTypes()[0].getName() + " to match the Builder method.");
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(parentCls.getName() + " must contain a getter method of " +
                    "signature " + expectedSignature + " to match the Builder setter method.");
        }
    }

    // We're acting on a Row class
    FieldDelegate(@Nullable Field field,
                  @Nullable Method method,
                  @NonNull Class<?> rowType) {
        if (field == null && method == null)
            throw new IllegalStateException("Both the given field and method are null.");

        this.field = field;
        this.getterMethod = method;
        if (ignore()) return;

        if (this.field != null) {
            this.field.setAccessible(true);
        } else {
            getterMethod.setAccessible(true);
            if (getterMethod.getReturnType() == Void.class) {
                throw new IllegalStateException("Column getter methods cannot be return void.");
            }
            String targetName = getterMethod.getName();
            if (targetName.startsWith("get"))
                targetName = "set" + targetName.substring(3);
            final String setterSignature = "void " + targetName + "(" + getterMethod.getReturnType().getName() + ")";
            try {
                setterMethod = rowType.getDeclaredMethod(targetName, getterMethod.getReturnType());
                if (setterMethod.getReturnType() == void.class) {
                    setterMethod.setAccessible(true);
                } else {
                    throw new IllegalStateException("Column getter method " + getterMethod.getName() +
                            " does not have an equivalent setter method with signature " + setterSignature);
                }
            } catch (NoSuchMethodException e) {
                setterMethod = null;
                String targetFieldName = getterMethod.getName();
                if (targetFieldName.startsWith("get"))
                    targetFieldName = targetFieldName.substring(3);
                try {
                    this.field = rowType.getDeclaredField(targetFieldName);
                    if (this.field.getType().isAssignableFrom(getterMethod.getReturnType())) {
                        this.field.setAccessible(true);
                    } else {
                        throw new IllegalStateException("Column getter method " + getterMethod.getName() +
                                " does not have an equivalent setter method with signature " + setterSignature);
                    }
                } catch (NoSuchFieldException e1) {
                    throw new IllegalStateException("Column getter method " + getterMethod.getName() +
                            " does not have an equivalent setter method with signature " + setterSignature);
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions") String originalName() {
        if (getterMethod != null) return getterMethod.getName();
        else return field.getName();

    }

    @SuppressWarnings("ConstantConditions") public String name() {
        Column colAnn;
        String name;
        if (getterMethod != null) {
            colAnn = getterMethod.getAnnotation(Column.class);
            name = getterMethod.getName();
            if (name.startsWith("get"))
                name = name.substring(3);
        } else {
            colAnn = field.getAnnotation(Column.class);
            name = field.getName();
        }
        if (colAnn != null && colAnn.name() != null && !colAnn.name().trim().isEmpty()) {
            name = colAnn.name();
        }
        return name;
    }

    public boolean isId() {
        return name().equals("_id");
    }

    @SuppressWarnings("ConstantConditions") boolean ignore() {
        return !isId() && !isForeignKey() && getColumn() == null;
    }

    boolean isForeignKey() {
        return getForeignKey() != null;
    }

    @SuppressWarnings("ConstantConditions") ForeignKey getForeignKey() {
        if (getterMethod != null) {
            return getterMethod.getAnnotation(ForeignKey.class);
        } else {
            return field.getAnnotation(ForeignKey.class);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public Column getColumn() {
        if (getterMethod != null) {
            return getterMethod.getAnnotation(Column.class);
        } else {
            return field.getAnnotation(Column.class);
        }
    }

    @SuppressWarnings("ConstantConditions") Class<?> getType() {
        if (getterMethod != null) {
            return getterMethod.getReturnType();
        } else {
            return field.getType();
        }
    }

    @SuppressWarnings("ConstantConditions") Type getGenericType() {
        if (getterMethod != null) {
            return getterMethod.getGenericReturnType();
        } else {
            return field.getGenericType();
        }
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"}) @Nullable <T> T get(Object row) {
        if (getterMethod != null) {
            try {
                return (T) getterMethod.invoke(row);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to access Column getter method " + getterMethod.getName() + "()", e);
            }
        } else {
            try {
                return (T) field.get(row);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to access Column getter field " + field.getName(), e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"}) void set(Object row, Object value) {
        if (setterMethod != null) {
            try {
                setterMethod.invoke(row, value);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to access Column setter method " + setterMethod.getName() + "(Object)", e);
            }
        } else {
            try {
                field.set(row, value);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to access Column setter field " + field.getName(), e);
            }
        }
    }

    @Nullable String schema() {
        if (isForeignKey()) return null;
        else if (ignore()) return null;
        Column columnAnnotation = getColumn();
        StringBuilder colName = new StringBuilder(name());
        colName.append(" ");
        colName.append(getClassTypeString(getType()));
        if (columnAnnotation.primaryKey())
            colName.append(" PRIMARY KEY");
        if (columnAnnotation.autoIncrement())
            colName.append(" AUTOINCREMENT");
        if (columnAnnotation.notNull())
            colName.append(" NOT NULL");
        return colName.toString();
    }

    private String getClassTypeString(Class<?> cls) {
        if (cls.equals(String.class) || cls.equals(char[].class) || cls.equals(Character[].class)) {
            return "TEXT";
        } else if (cls.equals(Float.class) || cls.equals(float.class) ||
                cls.equals(Double.class) || cls.equals(double.class)) {
            return "REAL";
        } else if (cls.equals(Integer.class) || cls.equals(int.class) ||
                cls.equals(Long.class) || cls.equals(long.class) ||
                cls.equals(Boolean.class) || cls.equals(boolean.class)) {
            return "INTEGER";
        } else {
            return "BLOB";
        }
    }
}
