package com.manabrew.network;

import com.manabrew.inventory.*;
import com.manabrew.model.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GameRoom {

    // ── constants ─────────────────────────────────────────────────────────────
    private static final int STARTING_GOLD = 100;

    // ── identity ──────────────────────────────────────────────────────────────
    private final String roomCode;

    // ── shared state ──────────────────────────────────────────────────────────
    public  Pantry                          roomPantry;
    public  StorageBunker<OrderTicket>      orders  = new StorageBunker<>();
    public  CopyOnWriteArrayList<ClientHandler> players = new CopyOnWriteArrayList<>();

    private int         roundNum          = 1;
    private AtomicInteger vaultGold       = new AtomicInteger(STARTING_GOLD);
    private GamePhase   phase             = GamePhase.SHOP;
    public  String      hostPlayer        = "";

    // quota tracking
    private int currentQuota       = 0;
    private AtomicInteger goldEarnedThisRound = new AtomicInteger(0);

    public GameRoom(String code) {
        this.roomCode   = code;
        this.roomPantry = new Pantry();
    }

    // ── accessors ─────────────────────────────────────────────────────────────

    public String    getRoomCode()   { return roomCode; }
    public int       getRoundNum()   { return roundNum; }
    public int       getVaultGold()  { return vaultGold.get(); }
    public int       getCurrentQuota()       { return currentQuota; }
    public int       getGoldEarnedThisRound(){ return goldEarnedThisRound.get(); }
    public GamePhase getPhase()      { return phase; }

    /** Convenience — kept so UI code reads naturally. */
    public boolean isShopPhase()    { return phase == GamePhase.SHOP; }

    // ── gold mutations (thread-safe) ──────────────────────────────────────────

    /**
     * Adds {@code amount} gold to the vault and to this round's earned total.
     * Safe to call from multiple threads simultaneously.
     */
    public void addGold(int amount) {
        vaultGold.addAndGet(amount);
        goldEarnedThisRound.addAndGet(amount);
    }

    /**
     * Deducts {@code amount} from the vault (floor at 0).
     * Safe to call from multiple threads simultaneously.
     */
    public synchronized void deductGold(int amount) {
        int current = vaultGold.get();
        vaultGold.set(Math.max(0, current - amount));
    }

    // ── quota ─────────────────────────────────────────────────────────────────

    /** Scales with round number and player count. */
    public int calculateQuota() {
        return (30 * roundNum) + (20 * players.size() * roundNum);
    }

    // ── player management ─────────────────────────────────────────────────────

    /** Adds a player; first one in becomes the host. */
    public void addPlayer(ClientHandler player, String username) {
        players.add(player);
        if (players.size() == 1) hostPlayer = username;
        broadcast("[room " + roomCode + "] " + username + " joined.  host: " + hostPlayer);
        broadcast("shop phase active. buy ingredients with  shop <item>.  host: type  start  when ready.");
    }

    /** Removes a disconnected player; promotes a new host if needed. */
    public void handleDisconnect(ClientHandler p) {
        players.remove(p);
        if (!players.isEmpty() && p.getUsername().equals(hostPlayer)) {
            hostPlayer = players.get(0).getUsername();
            broadcast("[alert] host left. new host: " + hostPlayer);
        }
    }

    // ── messaging ─────────────────────────────────────────────────────────────

    /** Sends a message to every player in the room. */
    public void broadcast(String msg) {
        for (ClientHandler ch : players) ch.sendMessage(msg);
    }

    // ── round lifecycle ───────────────────────────────────────────────────────

    /** Flips from shop phase to brew phase, locks in the quota, and starts the timer thread. */
    public void startRound() {
        if (phase != GamePhase.SHOP) return;
        phase = GamePhase.BREWING;

        currentQuota = calculateQuota();
        goldEarnedThisRound.set(0);

        broadcast(TerminalColors.YELLOW
                + "=== round " + roundNum + " started! quota: " + currentQuota + "g ==="
                + TerminalColors.RESET);

        new Thread(() -> {
            Random rng      = new Random();
            int    timeLeft = 60 + (roundNum * 10);

            while (timeLeft > 0 && !players.isEmpty()) {
                try {
                    Thread.sleep(1000);
                    timeLeft--;

                    int spawnEvery = Math.max(4, 15 - (roundNum * 2));
                    if (timeLeft % spawnEvery == 0) {
                        String type   = PotionFactory.ALL_TYPES[rng.nextInt(PotionFactory.ALL_TYPES.length)];
                        Potion newPot = PotionFactory.create(type);
                        orders.add(new OrderTicket(newPot, 45));
                        broadcast("[ new order ] --> " + newPot.getName());
                    }
                } catch (InterruptedException ignored) {}
            }

            if (goldEarnedThisRound.get() < currentQuota) {
                // Failed — reset to square one.
                phase = GamePhase.SHOP;
                broadcast(TerminalColors.RED + "=== GAME OVER! ===" + TerminalColors.RESET);
                broadcast("you only made " + goldEarnedThisRound.get() + "g out of the "
                        + currentQuota + "g required.");
                broadcast("the tavern went bankrupt. wiping everything and resetting back to round 1...");
                roundNum  = 1;
                vaultGold.set(STARTING_GOLD);
                roomPantry = new Pantry();
                orders     = new StorageBunker<>();
                for (ClientHandler p : players) p.clearDebuffs();
            } else {
                // Survived.
                phase = GamePhase.SHOP;
                roundNum++;
                for (ClientHandler p : players) p.clearDebuffs();
                broadcast(TerminalColors.GREEN + "=== quota met! ("
                        + goldEarnedThisRound.get() + "/" + currentQuota + "g) ==="
                        + TerminalColors.RESET);
                broadcast("vault: " + vaultGold.get() + "g. shop phase is back. host types  start  when ready.");
            }

        }).start();
    }
}