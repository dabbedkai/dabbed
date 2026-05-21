package com.manabrew.model;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class PotionFactory {

    // ── internal catalogue — MUST be declared first ───────────────────────────
    private static final Map<String, String[]> CATALOGUE = new LinkedHashMap<>();
    static {
        CATALOGUE.put("healing", new String[] { "Healing Elixir", "1", "15", "water", "lunar shard" });
        CATALOGUE.put("toxic", new String[] { "Toxic Brew", "2", "30", "dragon scale", "fairy dust" });
        CATALOGUE.put("fireball", new String[] { "Fireball", "3", "40", "dragon scale", "fire pepper" });
        CATALOGUE.put("mana", new String[] { "Mana Crystal", "1", "10", "water", "fairy dust" });
        CATALOGUE.put("shadow", new String[] { "Shadow Draft", "3", "50", "void extract", "dragon scale" });
        CATALOGUE.put("stardust", new String[] { "Stardust Flask", "2", "25", "fairy dust", "lunar shard" });
        CATALOGUE.put("volcano", new String[] { "Volcano Sludge", "3", "60", "fire pepper", "void extract" });
        CATALOGUE.put("moon-tear", new String[] { "Lunar Tear", "4", "100", "water", "void extract" });
        CATALOGUE.put("eclipse", new String[] { "Eclipse", "4", "80", "void extract", "lunar shard" });
        CATALOGUE.put("phoenix", new String[] { "Phoenix Down", "5", "120", "fire pepper", "fairy dust" });
    }

    public static final String[] ALL_TYPES = loadTypes();

    // ── factory method ────────────────────────────────────────────────────────

    public static Potion create(String type) {
        String[] d = CATALOGUE.get(type.toLowerCase());
        if (d == null)
            return null;
        return new Potion(d[0], Integer.parseInt(d[1]), Integer.parseInt(d[2]),
                new Ingredient[] { new Ingredient(d[3]), new Ingredient(d[4]) });
    }

    // ── Recipes.json loader ───────────────────────────────────────────────────

    private static String[] loadTypes() {
        File f = new File("Recipes.json");
        if (f.exists()) {
            try (Reader r = new FileReader(f)) {
                JsonArray arr = JsonParser.parseReader(r).getAsJsonArray();
                List<String> types = new ArrayList<>();
                for (JsonElement el : arr) {
                    String t = el.getAsJsonObject().get("type").getAsString().trim().toLowerCase();
                    if (CATALOGUE.containsKey(t)) {
                        types.add(t);
                    } else {
                        System.out.println("[warn] Recipes.json: unknown type '" + t + "' skipped.");
                    }
                }
                if (!types.isEmpty()) {
                    System.out.println("[info] Recipes.json loaded: " + types);
                    return types.toArray(new String[0]);
                }
            } catch (Exception e) {
                System.out
                        .println("[warn] Could not read Recipes.json, using full catalogue. (" + e.getMessage() + ")");
            }
        }
        // Fallback: use every type in the catalogue
        return CATALOGUE.keySet().toArray(new String[0]);
    }
}