package com.afollestad.inquiry;

import android.support.annotation.CheckResult;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.inquiry.lazyloading.LazyLoaderList;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
class Utils {

    @SuppressWarnings("unchecked")
    @CheckResult
    static <T> T newInstance(@NonNull Class<T> cls) {
        final Constructor ctor = getDefaultConstructor(cls);
        try {
            return (T) ctor.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate " + cls.getName(), t);
        }
    }

    @CheckResult
    private static Constructor<?> getDefaultConstructor(@NonNull Class<?> cls) {
        final Constructor[] ctors = cls.getDeclaredConstructors();
        Constructor ctor = null;
        for (Constructor ct : ctors) {
            ctor = ct;
            if (ctor.getGenericParameterTypes().length == 0)
                break;
        }
        if (ctor == null)
            throw new IllegalStateException("No default constructor found for " + cls.getName());
        ctor.setAccessible(true);
        return ctor;
    }

    static void closeQuietely(@Nullable Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    @IntRange(from = 0, to = Integer.MAX_VALUE)
    @CheckResult
    static int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle) count++;
        }
        return count;
    }

    @NonNull
    @CheckResult
    static String createArgsString(int argsCount) {
        StringBuilder sb = new StringBuilder(argsCount * 3 - 1);
        sb.append("(?");
        for (int i = 1; i < argsCount; i++)
            sb.append(", ?");
        sb.append(')');
        return sb.toString();
    }

    @NonNull
    @CheckResult
    static String join(boolean leadingComma, @Nullable String suffix, @NonNull Object... array) {
        final StringBuilder sb = new StringBuilder();
        if (leadingComma)
            sb.append(", ");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(array[i]);
            if (suffix != null) {
                sb.append(' ');
                sb.append(suffix);
            }
        }
        return sb.toString();
    }

    static String[] stringifyArray(@Nullable Object[] array) {
        if (array == null || array.length == 0) return null;
        final String[] result = new String[array.length];
        for (int i = 0; i < array.length; i++)
            result[i] = array[i] + "";
        return result;
    }

    static boolean classExtendsLazyLoader(Class<?> cls) {
        if (cls.equals(LazyLoaderList.class))
            return true;
        while (cls.getSuperclass() != null)
            cls = cls.getSuperclass();
        return cls.equals(LazyLoaderList.class);
    }

    static boolean classImplementsList(Class<?> cls) {
        if (cls.equals(List.class))
            return true;
        Class[] is = cls.getInterfaces();
        for (Class i : is)
            if (i.equals(List.class))
                return true;
        return false;
    }

    private static String getClassName(Type type) {
        String fullName = type.toString();
        if (fullName.startsWith("class "))
            return fullName.substring("class ".length());
        return fullName;
    }

    static Class<?> getGenericTypeOfField(Field field) {
        if (field.getType().isArray()) {
            return field.getType().getComponentType();
        } else if (classImplementsList(field.getType())) {
            Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            try {
                return Class.forName(getClassName(type));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to find class for " + getClassName(type));
            }
        } else {
            return field.getType();
        }
    }

    static void wrapInReIfNecessary(Throwable t) throws RuntimeException {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else {
            throw new RuntimeException(t);
        }
    }
}