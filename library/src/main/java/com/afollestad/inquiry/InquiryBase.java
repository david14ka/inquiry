package com.afollestad.inquiry;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess")
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class InquiryBase {

    private HashMap<String, FieldDelegate> idProxyCache;
    private HashMap<String, Class<?>> builderClassCache;
    private HashMap<String, Constructor<?>> constructorCache;
    private HashMap<String, Method> buildMethodCache;

    InquiryBase(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context can't be null.");
        }
        this.idProxyCache = new HashMap<>(0);
        this.builderClassCache = new HashMap<>(0);
        this.constructorCache = new HashMap<>(0);
        this.buildMethodCache = new HashMap<>(0);
    }

    public HashMap<String, FieldDelegate> getIdProxyCache() {
        return idProxyCache;
    }

    public HashMap<String, Class<?>> getBuilderClassCache() {
        return builderClassCache;
    }

    public HashMap<String, Constructor<?>> getConstructorCache() {
        return constructorCache;
    }

    public HashMap<String, Method> getBuildMethodCache() {
        return buildMethodCache;
    }

    public void destroyInstance() {
        if (idProxyCache != null) {
            idProxyCache.clear();
            idProxyCache = null;
        }
        if (builderClassCache != null) {
            builderClassCache.clear();
            builderClassCache = null;
        }
        if (constructorCache != null) {
            constructorCache.clear();
            constructorCache = null;
        }
        if (buildMethodCache != null) {
            buildMethodCache.clear();
            buildMethodCache = null;
        }
    }
}
