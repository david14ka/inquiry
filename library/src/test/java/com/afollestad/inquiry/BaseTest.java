package com.afollestad.inquiry;

import org.junit.Before;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Aidan Follestad (afollestad)
 */
class BaseTest {

    HashMap<String, FieldDelegate> idProxyCache;
    HashMap<String, Class<?>> builderClassCache;
    HashMap<String, Constructor<?>> constructorCache;
    HashMap<String, Method> buildMethodCache;
    HashMap<String, Method> withIdMethodCache;

    Inquiry mockInquiry;
    Query mockQuery;

    @Before public void setup() {
        idProxyCache = new HashMap<>(0);
        builderClassCache = new HashMap<>(0);
        constructorCache = new HashMap<>(0);
        buildMethodCache = new HashMap<>(0);

        mockInquiry = mock(Inquiry.class);
        when(mockInquiry.getIdProxyCache()).thenReturn(idProxyCache);
        when(mockInquiry.getBuilderClassCache()).thenReturn(builderClassCache);
        when(mockInquiry.getConstructorCache()).thenReturn(constructorCache);
        when(mockInquiry.getBuildMethodCache()).thenReturn(buildMethodCache);
        when(mockInquiry.getWithIdMethodCache()).thenReturn(withIdMethodCache);

        mockQuery = mock(Query.class);
        when(mockQuery.getInquiryInstance()).thenReturn(mockInquiry);
    }
}
