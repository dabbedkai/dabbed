package com.manabrew.inventory;

import java.util.ArrayList;

public class StorageBunker<T> {

    private final ArrayList<T> items = new ArrayList<>();

    public synchronized void add(T item) {
        items.add(item);
    }

    public synchronized boolean remove(T item) {
        return items.remove(item);
    }

    public synchronized ArrayList<T> getSnapshot() {
        return new ArrayList<>(items);
    }
}
