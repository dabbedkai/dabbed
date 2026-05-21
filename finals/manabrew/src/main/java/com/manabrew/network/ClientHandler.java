package com.manabrew.network;

import com.manabrew.inventory.Pantry;
import com.manabrew.model.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {

    // ── penalty durations ─────────────────────────────────────────────────────
    private static final long STUN_DURATION_MS        = 3_000L;
    private static final long RECIPE_BLIND_DURATION_MS = 3_000L;

    // ── player state ──────────────────────────────────────────────────────────
    private final Socket myConnection;
    private PrintWriter  outBuffer;
    private String       playerName;
    private GameRoom     myRoom          = null;
    private String       currentRoomCode = "";
    private int          personalOrdersDone = 0;

    // volatile penalty timers (epoch-ms)
    private long stunnedUntil         = 0;
    private long recipeBookHiddenUntil = 0;
    private long brewingUntil          = 0;  // player cannot brew another potion until this time

    // rolling event log displayed inside the UI box (LOG_MAX == UIRenderer's LOG_DISPLAY)
    private final LinkedList<String> recentLog = new LinkedList<>();
    private static final int LOG_MAX = 6;

    public ClientHandler(Socket s) {
        this.myConnection = s;
    }

    public String getUsername() { return playerName; }

    // ── message routing ───────────────────────────────────────────────────────

    public synchronized void sendMessage(String m) {
        if (outBuffer == null) return;
        synchronized (recentLog) {
            if (recentLog.size() >= LOG_MAX) recentLog.removeFirst();
            recentLog.add(m);
        }
        if (myRoom != null) drawUI();
        else outBuffer.println(m);
    }

    /** Sends raw text without going through the log or redraw cycle. */
    private void raw(String m) {
        if (outBuffer != null) outBuffer.println(m);
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            outBuffer = new PrintWriter(myConnection.getOutputStream(), true);
            BufferedReader inReader = new BufferedReader(
                    new InputStreamReader(myConnection.getInputStream()));

            raw(UIRenderer.buildTutorial());

            raw(TerminalColors.CYAN + "\nenter your alchemist name: " + TerminalColors.RESET);
            playerName = inReader.readLine();
            if (playerName == null || playerName.isBlank()) playerName = "Wanderer";

            raw(TerminalColors.YELLOW + "welcome, " + playerName + "." + TerminalColors.RESET);
            raw("type  'join <code>'  to enter or create a lobby (e.g. join 1234)\n");

            String cmdLine;
            while ((cmdLine = inReader.readLine()) != null) {
                cmdLine = cmdLine.trim();
                if (cmdLine.equalsIgnoreCase("quit")) break;
                handleCommand(cmdLine);
            }

        } catch (Exception ex) {
            if (myRoom != null) myRoom.handleDisconnect(this);
            System.out.println(TerminalColors.DIM + "log: " + playerName + " disconnected."
                    + TerminalColors.RESET);
        }
    }

    // ── command dispatcher ────────────────────────────────────────────────────

    private void handleCommand(String cmd) {
        // Ignore empty lines — the character-by-character client can send blank
        // lines on Enter with nothing typed, and Windows \r can arrive separately.
        if (cmd == null || cmd.isBlank()) return;

        String lower = cmd.toLowerCase();

        // Enforce stun penalty.
        if (myRoom != null && System.currentTimeMillis() < stunnedUntil) {
            long secs = (stunnedUntil - System.currentTimeMillis()) / 1000 + 1;
            sendMessage(TerminalColors.RED + "You are stunned and cannot act for " + secs + "s!"
                    + TerminalColors.RESET);
            return;
        }

        if      (lower.startsWith("join"))                      handleJoin(cmd);
        else if (lower.equals("start")   && myRoom != null)     handleStart();
        else if (lower.startsWith("shop") && myRoom != null)    handleShop(cmd);
        else if (lower.startsWith("claim") && myRoom != null)   handleClaim(cmd);
        else if (lower.startsWith("brew")  && myRoom != null)   handleBrew(cmd);
        else if (myRoom != null)
            sendMessage(TerminalColors.DIM + "unknown command. check the footer for what's available."
                    + TerminalColors.RESET);
        else
            raw("join a lobby first: join <code>");
    }

    // ── command handlers ──────────────────────────────────────────────────────

    private void handleJoin(String cmd) {
        String[] parts = cmd.split(" ", 2);
        if (parts.length < 2 || parts[1].isBlank()) { raw("usage: join <code>"); return; }

        currentRoomCode = parts[1].trim();
        myRoom          = Server.getOrCreateRoom(currentRoomCode);

        // Ensure the player's name is unique within the room.
        boolean nameTaken = true;
        while (nameTaken) {
            nameTaken = false;
            for (ClientHandler p : myRoom.players) {
                if (p.getUsername().equalsIgnoreCase(playerName)) {
                    nameTaken  = true;
                    playerName = playerName + new Random().nextInt(100);
                    break;
                }
            }
        }

        myRoom.addPlayer(this, playerName);
        drawUI();
    }

    private void handleStart() {
        if (myRoom.hostPlayer.equals(playerName)) {
            myRoom.startRound();
        } else {
            sendMessage(TerminalColors.RED + "only the host (" + myRoom.hostPlayer
                    + ") can start the round." + TerminalColors.RESET);
        }
    }

    private void handleShop(String cmd) {
        if (!myRoom.isShopPhase()) {
            sendMessage(TerminalColors.RED + "shop is closed while a round is active." + TerminalColors.RESET);
            return;
        }

        String[] parts = cmd.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            sendMessage(TerminalColors.RED + "usage: shop <item> [qty]" + TerminalColors.RESET);
            return;
        }

        String rawItem = parts[1].trim().toLowerCase();
        String item    = rawItem;
        int    qty     = 1;

        // Parse optional quantity at the end (e.g. "shop water 3").
        int lastSpace = rawItem.lastIndexOf(' ');
        if (lastSpace != -1) {
            String lastWord = rawItem.substring(lastSpace + 1);
            try {
                qty  = Integer.parseInt(lastWord);
                item = rawItem.substring(0, lastSpace).trim();
            } catch (NumberFormatException ignored) {}
        }

        if (qty <= 0) {
            sendMessage(TerminalColors.RED + "quantity must be at least 1." + TerminalColors.RESET);
            return;
        }

        int unitPrice = Pantry.getPrice(item);
        if (unitPrice < 0) {
            sendMessage(TerminalColors.RED + "'" + item + "' isn't sold here. check the recipe book."
                    + TerminalColors.RESET);
            return;
        }

        int totalCost = unitPrice * qty;

        // Use synchronized deductGold to avoid a race between two buyers.
        synchronized (myRoom) {
            if (myRoom.getVaultGold() < totalCost) {
                sendMessage(TerminalColors.RED + "not enough gold! need " + totalCost + "g, vault has "
                        + myRoom.getVaultGold() + "g." + TerminalColors.RESET);
                return;
            }
            myRoom.deductGold(totalCost);
        }

        myRoom.roomPantry.addStock(item, qty);
        myRoom.broadcast(TerminalColors.GREEN + playerName + " bought " + qty + "x " + item
                + " for " + totalCost + "g.  vault: " + myRoom.getVaultGold() + "g."
                + TerminalColors.RESET);
    }

    private void handleClaim(String cmd) {
        if (myRoom.isShopPhase()) {
            sendMessage(TerminalColors.RED + "no orders during shop phase. start the round first."
                    + TerminalColors.RESET);
            return;
        }

        String[] parts = cmd.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            sendMessage(TerminalColors.RED + "usage: claim <potion name>" + TerminalColors.RESET);
            return;
        }

        String  wanted = parts[1].trim();
        boolean found  = false;

        for (OrderTicket ticket : myRoom.orders.getSnapshot()) {
            if (ticket.getPotion().getName().equalsIgnoreCase(wanted)) {
                found = true;
                if (ticket.claim(playerName)) {
                    myRoom.broadcast(TerminalColors.BLUE + playerName + " claimed: " + wanted + "!"
                            + TerminalColors.RESET);
                } else {
                    sendMessage(TerminalColors.RED + ticket.getClaimedBy() + " already has that one."
                            + TerminalColors.RESET);
                }
                break;
            }
        }
        if (!found)
            sendMessage(TerminalColors.RED + "no order called '" + wanted + "'. check your spelling."
                    + TerminalColors.RESET);
    }

    private void handleBrew(String cmd) {
        if (myRoom.isShopPhase()) {
            sendMessage(TerminalColors.RED + "can't brew during shop phase." + TerminalColors.RESET);
            return;
        }

        // Check if still brewing a previous potion.
        if (System.currentTimeMillis() < brewingUntil) {
            long secsLeft = (brewingUntil - System.currentTimeMillis()) / 1000 + 1;
            sendMessage(TerminalColors.YELLOW + "still brewing... " + secsLeft + "s remaining."
                    + TerminalColors.RESET);
            return;
        }

        String[] parts = cmd.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            sendMessage(TerminalColors.RED + "usage: brew <ing1,ing2>" + TerminalColors.RESET);
            return;
        }

        String[] typed    = parts[1].trim().split(",");
        boolean  hasTypo  = false;

        for (int i = 0; i < typed.length; i++) {
            typed[i] = typed[i].trim().toLowerCase();
            if (typed[i].isEmpty()) {
                sendMessage(TerminalColors.RED + "invalid ingredient format. usage: brew <ing1,ing2>"
                        + TerminalColors.RESET);
                return;
            }
            if (!Pantry.isKnownIngredient(typed[i])) hasTypo = true;
        }

        if (hasTypo) {
            triggerExplosion("Typo in ingredients caused a volatile reaction!");
            return;
        }

        Arrays.sort(typed);

        // Find the matching claimed order.
        OrderTicket target = null;
        for (OrderTicket tk : myRoom.orders.getSnapshot()) {
            String[] req = toNames(tk.getPotion().getRecipe());
            Arrays.sort(req);
            if (Arrays.equals(typed, req)) { target = tk; break; }
        }

        if (target == null) {
            sendMessage(TerminalColors.RED + "no active order matches those ingredients."
                    + TerminalColors.RESET);
            return;
        }
        if (!playerName.equals(target.getClaimedBy())) {
            sendMessage(TerminalColors.RED + "claim it first: claim " + target.getPotion().getName()
                    + TerminalColors.RESET);
            return;
        }
        if (!myRoom.roomPantry.takeIngredients(typed)) {
            sendMessage(TerminalColors.RED + "pantry is short on stock. wait for the next shop phase."
                    + TerminalColors.RESET);
            return;
        }

        // Volatile combo check.
        if (typed.length == 2 && Brewable.isVolatile(typed[0], typed[1])) {
            if (Math.random() < 0.5) {
                triggerExplosion("Volatile combo became unstable!");
                return;
            } else {
                sendMessage(TerminalColors.YELLOW + "⚠ volatile combo handled safely..." + TerminalColors.RESET);
            }
        }

        // Mark the player as brewing (non-blocking — no Thread.sleep on this thread).
        int  brewSecs = target.getPotion().getTier() * 2;
        brewingUntil  = System.currentTimeMillis() + (brewSecs * 1000L);
        final OrderTicket finalTarget = target;

        myRoom.broadcast(TerminalColors.CYAN + "⚗  " + playerName + " is brewing "
                + target.getPotion().getName() + "...  (" + brewSecs + "s)"
                + TerminalColors.RESET);

        // Brew timer runs on its own thread so this handler stays responsive.
        new Thread(() -> {
            try {
                Thread.sleep(brewSecs * 1000L);
            } catch (InterruptedException ignored) {}

            if (myRoom.orders.remove(finalTarget)) {
                int reward = finalTarget.getPotion().getPrice();
                myRoom.addGold(reward);          // thread-safe
                personalOrdersDone++;
                Server.potionsDelivered.incrementAndGet();
                Server.totalGold.addAndGet(reward);
                myRoom.broadcast(TerminalColors.GREEN + "✓  " + playerName + " delivered "
                        + finalTarget.getPotion().getName() + "!  +" + reward + "g   vault: "
                        + myRoom.getVaultGold() + "g." + TerminalColors.RESET);
            }
        }).start();
    }

    // ── explosion mechanic ────────────────────────────────────────────────────

    private void triggerExplosion(String reason) {
        sendMessage(TerminalColors.RED + "💥 BOOM! " + reason + TerminalColors.RESET);
        Random rng         = new Random();
        int    penaltyType = rng.nextInt(3);

        switch (penaltyType) {
            case 0:
                int loss = 15 + rng.nextInt(16);
                myRoom.deductGold(loss);     // thread-safe
                myRoom.broadcast(TerminalColors.RED + playerName
                        + "'s cauldron exploded! Team lost " + loss + "g."
                        + TerminalColors.RESET);
                break;
            case 1:
                recipeBookHiddenUntil = System.currentTimeMillis() + RECIPE_BLIND_DURATION_MS;
                sendMessage(TerminalColors.MAGENTA
                        + "Soot covers your eyes! Recipes and orders are unreadable for 3s."
                        + TerminalColors.RESET);
                break;
            case 2:
                stunnedUntil = System.currentTimeMillis() + STUN_DURATION_MS;
                sendMessage(TerminalColors.YELLOW
                        + "You are stunned by the blast and can't act for 3s!"
                        + TerminalColors.RESET);
                break;
        }
        drawUI();
    }

    // ── debuff management ─────────────────────────────────────────────────────

    /** Clears all active penalties. Called by GameRoom at the end of every round. */
    public void clearDebuffs() {
        stunnedUntil         = 0;
        recipeBookHiddenUntil = 0;
        brewingUntil         = 0;
    }

    // ── screen drawing ────────────────────────────────────────────────────────

    synchronized void drawUI() {
        if (outBuffer == null || myRoom == null) return;
        List<String> logSnapshot;
        synchronized (recentLog) {
            logSnapshot = new ArrayList<>(recentLog);
        }
        String screen = UIRenderer.buildScreen(
                playerName, personalOrdersDone, currentRoomCode,
                myRoom, logSnapshot, recipeBookHiddenUntil);
        outBuffer.print(screen);
        outBuffer.flush();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String[] toNames(Ingredient[] ings) {
        String[] names = new String[ings.length];
        for (int i = 0; i < ings.length; i++) names[i] = ings[i].getName().toLowerCase();
        return names;
    }
}