package com.afollestad.inquiry;

import android.database.Cursor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Aidan Follestad (afollestad)
 */
@RunWith(MockitoJUnitRunner.class)
public class BuilderConverterTests extends BaseTest {

    @Test public void test_basic_row_to_values() {
        BuilderRow row = BuilderRow.create()
                .username("afollestad")
                .age(21)
                .online(true)
                .rank(100f)
                .build();

        Map<Object, FieldDelegate> foreignChildrenMap = new HashMap<>(0);
        List<FieldDelegate> proxiesList = Converter.classFieldDelegates(
                row.getClass(), false, BuilderRow.Builder.class);
        RowValues values = Converter.classToValues(row, null, proxiesList, foreignChildrenMap);

        assertEquals(foreignChildrenMap.size(), 0);
        assertEquals(proxiesList.size(), 5);
        assertNotNull(values);

        // _id should not exist in this set since it is autoIncrement
        assertNull(values.getLong("_id"));

        assertEquals(values.getString("username"), "afollestad");
        assertEquals((int) values.getInt("age"), 21);
        assertTrue(values.getBool("online"));
        assertEquals(values.getFloat("rank"), 100f);
    }

    @Test public void test_cursor_to_row() throws Exception {
        Cursor mockCursor = mock(Cursor.class);
        when(mockCursor.getColumnCount()).thenReturn(5);

        when(mockCursor.getType(0)).thenReturn(Cursor.FIELD_TYPE_INTEGER);
        when(mockCursor.getType(1)).thenReturn(Cursor.FIELD_TYPE_STRING);
        when(mockCursor.getType(2)).thenReturn(Cursor.FIELD_TYPE_INTEGER);
        when(mockCursor.getType(3)).thenReturn(Cursor.FIELD_TYPE_INTEGER);
        when(mockCursor.getType(4)).thenReturn(Cursor.FIELD_TYPE_FLOAT);

        when(mockCursor.getLong(0)).thenReturn(50L);
        when(mockCursor.getString(1)).thenReturn("waverlysummer");
        when(mockCursor.getInt(2)).thenReturn(19);
        when(mockCursor.getInt(3)).thenReturn(1);
        when(mockCursor.getFloat(4)).thenReturn(99F);

        when(mockCursor.getColumnName(0)).thenReturn("_id");
        when(mockCursor.getColumnName(1)).thenReturn("username");
        when(mockCursor.getColumnName(2)).thenReturn("age");
        when(mockCursor.getColumnName(3)).thenReturn("online");
        when(mockCursor.getColumnName(4)).thenReturn("rank");

        Class<?> builderCls = BuilderRow.Builder.class;
        Method buildMethod = builderCls.getDeclaredMethod("build");

        builderClassCache.put(BuilderRow.class.getName(), builderCls);
        buildMethodCache.put(builderCls.getName(), buildMethod);

        when(mockInquiry.getBuilderClass(BuilderRow.class))
                .thenReturn((Class) builderCls);
        when(mockInquiry.getBuildMethod(BuilderRow.class, builderCls))
                .thenReturn(buildMethod);

        BuilderRow row = Converter.cursorToObject(mockQuery, mockCursor, BuilderRow.class);

        assertEquals(row.id(), 50);
        assertEquals(row.username(), "waverlysummer");
        assertEquals(row.age(), 19);
        assertTrue(row.online());
        assertEquals(row.rank(), 99F);
    }
}
