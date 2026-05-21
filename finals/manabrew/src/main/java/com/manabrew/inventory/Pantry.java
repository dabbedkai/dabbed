package com.manabrew.inventory;

import java.util.HashMap;
import java.util.Map;

public class Pantry {

    // Price lookup for the shop — static so it never changes at runtime.
    private static final Map<String, Integer> PRICES = new HashMap<>();
    static {
        PRICES.put("water", 5);
        PRICES.put("dragon scale", 15);
        PRICES.put("fairy dust", 12);
        PRICES.put("fire pepper", 10);
        PRICES.put("lunar shard", 20);
        PRICES.put("void extract", 25);
    }

    private final HashMap<String, Integer> stock = new HashMap<>();

    public Pantry() {
        // Reduced starting stock so the shop phase actually matters.
        stock.put("water", 10);
        stock.put("dragon scale", 5);
        stock.put("fairy dust", 5);
        stock.put("fire pepper", 5);
        // Rarer ingredients start at zero — players must buy them.
        stock.put("lunar shard", 0);
        stock.put("void extract", 0);
    }

    public static int getPrice(String item) {
        return PRICES.getOrDefault(normalise(item), -1);
    }

    /** Returns {@code true} if the item name is recognised in the price list. */
    public static boolean isKnownIngredient(String item) {
        return PRICES.containsKey(normalise(item));
    }

    /** Current shelf quantity for an item. */
    public synchronized int getStock(String item) {
        return stock.getOrDefault(normalise(item), 0);
    }

    /** Adds {@code qty} units; creates the entry if it never existed. */
    public synchronized void addStock(String item, int qty) {
        String key = normalise(item);
        stock.put(key, stock.getOrDefault(key, 0) + qty);
    }
    
    /**
     * Atomically checks if all items are available and, if so, deducts them from stock.
     * Returns {@code true} if the operation succeeded, or {@code false} if any item was unavailable.
     */
    public synchronized boolean takeIngredients(String[] items) {
        // First pass: verify everything is available.
        for (String item : items) {
            if (stock.getOrDefault(normalise(item), 0) <= 0)
                return false;
        }
        // Second pass: deduct.
        for (String item : items) {
            String key = normalise(item);
            stock.put(key, stock.get(key) - 1);
        }
        return true;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String normalise(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
