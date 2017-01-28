package com.afollestad.inquiry;

import java.util.Iterator;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
class ValuesWrapper<T> implements Iterable<T> {

    private List<T> list;
    private T[] array;

    ValuesWrapper(List<T> list) {
        this.list = list;
    }

    ValuesWrapper(T[] array) {
        this.array = array;
    }

    public T get(int index) {
        if (list != null) return list.get(index);
        else if (array != null) return array[index];
        else return null;
    }

    public void set(int index, T value) {
        if (list != null) list.set(index, value);
        else array[index] = value;
    }

    public boolean isNull(int index) {
        if (list != null) return list.get(index) == null;
        else return array[index] == null;
    }

    public int size() {
        if (list != null) return list.size();
        else if (array != null) return array.length;
        else return 0;
    }

    @Override public Iterator<T> iterator() {
        if (list != null) {
            return list.iterator();
        } else {
            return new Iterator<T>() {

                int index = -1;

                @Override public boolean hasNext() {
                    return index + 1 < array.length;
                }

                @Override public T next() {
                    index++;
                    return array[index];
                }
            };
        }
    }
}
