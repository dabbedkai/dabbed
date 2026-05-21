package com.manabrew.model;

public class OrderTicket {

    private final Potion targetPotion;
    private int timeLeft;
    private String claimedBy; // null = unclaimed; otherwise holds the player's username

    public OrderTicket(Potion targetPotion, int maxTime) {
        this.targetPotion = targetPotion;
        this.timeLeft = maxTime;
        this.claimedBy = null;
    }

    public Potion getPotion() {
        return targetPotion;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void tickTimer() {
        timeLeft--;
    }

    public synchronized boolean claim(String player) {
        if (claimedBy == null) {
            claimedBy = player;
            return true;
        }
        return claimedBy.equals(player);
    }

    public synchronized String getClaimedBy() {
        return claimedBy;
    }
}
