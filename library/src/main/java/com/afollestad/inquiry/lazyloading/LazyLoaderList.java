package com.afollestad.inquiry.lazyloading;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.afollestad.inquiry.AnyPredicate;
import com.afollestad.inquiry.Inquiry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Aidan Follestad (afollestad)
 */
public class LazyLoaderList<RT> implements List<RT> {

    protected Inquiry inquiry;
    protected String tableName;
    protected String foreignColumnName;
    protected String inverseFieldName;
    protected Object row;
    protected Class<?> childType;
    protected List<RT> items;

    private boolean mDidLazyLoad;

    public LazyLoaderList() {
        items = new ArrayList<>();
    }

    public LazyLoaderList(int capacity) {
        items = new ArrayList<>(capacity);
    }

    protected LazyLoaderList(Inquiry inquiry, String tableName, String foreignColumnName, String inverseFieldName, Object row, Class<?> childType) {
        this.inquiry = inquiry;
        this.tableName = tableName;
        this.foreignColumnName = foreignColumnName;
        this.inverseFieldName = inverseFieldName;
        this.row = row;
        this.childType = childType;
        items = new ArrayList<>();
    }

    private void lazyLoadIfNecessary() {
        if (mDidLazyLoad || inquiry == null)
            return;
        if (inquiry.isDestroyed())
            throw new IllegalStateException("This LazyLoaderList is not attached to a non-destroyed Inquiry instance.");
        Log.d("LazyLoaderList", "Lazy loading " + tableName + " for row type " + childType.getName());
        _performLazyLoad();
        mDidLazyLoad = true;
    }

    public boolean didLazyLoad() {
        return mDidLazyLoad;
    }

    protected void _performLazyLoad() {
        // To be overridden
    }

    public boolean any() {
        return !isEmpty();
    }

    public boolean any(@NonNull AnyPredicate<RT> predicate) {
        for (int i = 0; i < items.size(); i++) {
            if (predicate.match(items.get(i)))
                return true;
        }
        return false;
    }

    public boolean none() {
        return isEmpty();
    }

    public boolean none(@NonNull AnyPredicate<RT> predicate) {
        for (int i = 0; i < items.size(); i++) {
            if (predicate.match(items.get(i)))
                return false;
        }
        return true;
    }

    @Nullable
    public RT first() {
        if (isEmpty()) return null;
        return items.get(0);
    }

    @Nullable
    public RT first(AnyPredicate<RT> predicate) {
        if (isEmpty()) return null;
        for (int i = 0; i < items.size(); i++) {
            if (predicate.match(items.get(i)))
                return items.get(i);
        }
        return null;
    }

    @Nullable
    public RT last() {
        if (isEmpty()) return null;
        return items.get(items.size() - 1);
    }

    @Nullable
    public RT last(AnyPredicate<RT> predicate) {
        if (isEmpty()) return null;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (predicate.match(items.get(i)))
                return items.get(i);
        }
        return null;
    }

    @Override
    public int size() {
        lazyLoadIfNecessary();
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        lazyLoadIfNecessary();
        return items.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        lazyLoadIfNecessary();
        return items.contains(o);
    }

    @NonNull
    @Override
    public Iterator<RT> iterator() {
        lazyLoadIfNecessary();
        return items.iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        lazyLoadIfNecessary();
        return items.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] ts) {
        lazyLoadIfNecessary();
        return items.toArray(ts);
    }

    @Override
    public boolean add(RT rt) {
        lazyLoadIfNecessary();
        return items.add(rt);
    }

    @Override
    public boolean remove(Object o) {
        lazyLoadIfNecessary();
        return items.remove(o);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        lazyLoadIfNecessary();
        return items.containsAll(collection);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends RT> collection) {
        lazyLoadIfNecessary();
        return items.addAll(collection);
    }

    @Override
    public boolean addAll(int i, @NonNull Collection<? extends RT> collection) {
        lazyLoadIfNecessary();
        return items.addAll(i, collection);
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        lazyLoadIfNecessary();
        return items.removeAll(collection);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        lazyLoadIfNecessary();
        return items.retainAll(collection);
    }

    @Override
    public void clear() {
        lazyLoadIfNecessary();
        items.clear();
    }

    @Override
    public RT get(int i) {
        lazyLoadIfNecessary();
        return items.get(i);
    }

    @Override
    public RT set(int i, RT rt) {
        lazyLoadIfNecessary();
        return items.set(i, rt);
    }

    @Override
    public void add(int i, RT rt) {
        lazyLoadIfNecessary();
        items.add(i, rt);
    }

    @Override
    public RT remove(int i) {
        lazyLoadIfNecessary();
        return items.remove(i);
    }

    @Override
    public int indexOf(Object o) {
        lazyLoadIfNecessary();
        return items.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        lazyLoadIfNecessary();
        return items.indexOf(o);
    }

    @Override
    public ListIterator<RT> listIterator() {
        lazyLoadIfNecessary();
        return items.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<RT> listIterator(int i) {
        lazyLoadIfNecessary();
        return items.listIterator(i);
    }

    @NonNull
    @Override
    public List<RT> subList(int i, int i1) {
        lazyLoadIfNecessary();
        return items.subList(i, i1);
    }

    @Override
    public String toString() {
        return "LazyLoader - " + childType + " - " + size() + " items";
    }
}