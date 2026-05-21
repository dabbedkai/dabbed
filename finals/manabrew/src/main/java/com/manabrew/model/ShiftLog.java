package com.manabrew.model;

/** Plain data object — Gson serialises/deserialises this to/from shift_logs.json. */
public class ShiftLog {
    private final String timestamp;
    private final int    potionsDelivered;
    private final int    finalGold;

    public ShiftLog(String timestamp, int potionsDelivered, int finalGold) {
        this.timestamp        = timestamp;
        this.potionsDelivered = potionsDelivered;
        this.finalGold        = finalGold;
    }

    public String getTimestamp()        { return timestamp;        }
    public int    getPotionsDelivered() { return potionsDelivered; }
    public int    getFinalGold()        { return finalGold;        }
}
