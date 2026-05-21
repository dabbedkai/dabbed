package com.manabrew.model;

public class Potion implements Brewable {

    private final String name;
    private final int tier;
    private final int price;
    private final Ingredient[] recipe;

    public Potion(String name, int tier, int price, Ingredient[] recipe) {
        this.name = name;
        this.tier = tier;
        this.price = price;
        this.recipe = recipe;
    }

    public String getName() {
        return name;
    }

    public int getTier() {
        return tier;
    }

    public int getPrice() {
        return price;
    }

    public Ingredient[] getRecipe() {
        return recipe;
    }

    /** Comma-separated readable ingredient list for the UI. */
    public String getRecipeString() {
        StringBuilder sb = new StringBuilder();
        for (Ingredient i : recipe)
            sb.append(i.getName()).append(", ");
        return sb.substring(0, sb.length() - 2);
    }
}
