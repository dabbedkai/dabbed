package com.manabrew.model;

// simple value object representing one ingredient in a recipe
public class Ingredient {
    private final String name;

    public Ingredient(String name) {
        this.name = name.trim().toLowerCase();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
