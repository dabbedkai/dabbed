package com.manabrew.network;

import com.manabrew.model.*;
import com.manabrew.inventory.Pantry;
import java.util.List;

public class UIRenderer {

    // ── layout constants ──────────────────────────────────────────────────────
    private static final int BOX_W           = 72;
    private static final int CONTENT_W       = BOX_W - 2;
    private static final int MAX_ORDERS_SHOWN = 6;   // cap so orders never overflow the box
    private static final int LOG_DISPLAY      = 6;   // recent-log lines shown inside the box

    // ── public entry point ────────────────────────────────────────────────────

    public static String buildScreen(
            String       playerName,
            int          personalOrdersDone,
            String       currentRoomCode,
            GameRoom     myRoom,
            List<String> recentLog,
            long         recipeBookHiddenUntil) {

        StringBuilder sb = new StringBuilder();

        // Wipe terminal and snap cursor to top-left.
        sb.append(TerminalColors.CLEAR);

        // ── header ────────────────────────────────────────────────────────────
        sb.append(topBar("ManaBrew Tavern")).append("\n");

        String phaseTag = myRoom.isShopPhase()
                ? TerminalColors.YELLOW + "SHOP"    + TerminalColors.RESET
                : TerminalColors.RED    + "BREWING" + TerminalColors.RESET;

        sb.append(boxRow(
                TerminalColors.BOLD + playerName + TerminalColors.RESET
                + "   vault: " + TerminalColors.YELLOW + myRoom.getVaultGold() + "g" + TerminalColors.RESET
                + "   orders done: " + TerminalColors.GREEN + personalOrdersDone + TerminalColors.RESET
                + "   " + phaseTag
        )).append("\n");

        String quotaTag = myRoom.isShopPhase()
                ? "   next quota: " + TerminalColors.YELLOW + myRoom.calculateQuota() + "g" + TerminalColors.RESET
                : "   quota: "      + TerminalColors.YELLOW
                        + myRoom.getGoldEarnedThisRound() + "/" + myRoom.getCurrentQuota() + "g"
                        + TerminalColors.RESET;

        sb.append(boxRow(
                "room: " + TerminalColors.CYAN + currentRoomCode + TerminalColors.RESET
                + "   round: " + myRoom.getRoundNum()
                + "   host: "  + TerminalColors.YELLOW + myRoom.hostPlayer + TerminalColors.RESET
                + quotaTag
        )).append("\n");

        sb.append(divider()).append("\n");

        // ── main body ─────────────────────────────────────────────────────────
        if (myRoom.isShopPhase()) buildShopView(sb, myRoom, recipeBookHiddenUntil);
        else                      buildRoundView(sb, myRoom, recipeBookHiddenUntil);

        // ── recent log — inside the box so total height stays fixed ───────────
        sb.append(divider()).append("\n");
        sb.append(boxRow(TerminalColors.DIM + TerminalColors.BOLD
                + " RECENT" + TerminalColors.RESET)).append("\n");

        // Show at most LOG_DISPLAY lines; pad with empty rows so the box height
        // never shrinks between redraws (prevents the cursor jump effect).
        int start = Math.max(0, recentLog.size() - LOG_DISPLAY);
        int shown = 0;
        for (int i = start; i < recentLog.size(); i++, shown++) {
            sb.append(boxRow(" " + TerminalColors.stripAnsi(recentLog.get(i))
                    .substring(0, Math.min(TerminalColors.stripAnsi(recentLog.get(i)).length(), CONTENT_W - 2))
            )).append("\n");
        }
        // Pad remaining slots so height is constant
        for (int i = shown; i < LOG_DISPLAY; i++) sb.append(emptyRow()).append("\n");

        // ── footer hint ───────────────────────────────────────────────────────
        sb.append(divider()).append("\n");
        String hint = myRoom.isShopPhase()
                ? "shop <item> [qty] | start (host only)"
                : "claim <potion name> | brew <ing1,ing2>";
        sb.append(boxRow(TerminalColors.DIM + "> " + hint + TerminalColors.RESET)).append("\n");
        sb.append(bottomBar()).append("\n");

        // Prompt line — the client will reprint the input buffer after this.
        sb.append(TerminalColors.GREEN + "> " + TerminalColors.RESET);

        return sb.toString();
    }

    // ── tutorial screen ───────────────────────────────────────────────────────

    public static String buildTutorial() {
        StringBuilder t = new StringBuilder();
        t.append(TerminalColors.CLEAR);
        t.append(topBar("ManaBrew  -  Alchemist's Guide")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(boxRow("  you and your crew run a potion shop together.")).append("\n");
        t.append(boxRow("  hit the required gold quota before the timer ends")).append("\n");
        t.append(boxRow("  or the tavern goes bankrupt and you start over.")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(divider()).append("\n");
        t.append(boxRow(TerminalColors.YELLOW + "  THE TWO PHASES" + TerminalColors.RESET)).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(boxRow("  SHOP PHASE  ──  buy ingredients from the shared vault,")).append("\n");
        t.append(boxRow("                  check the recipe book, plan what you need.")).append("\n");
        t.append(boxRow("                  host types  start  when the team is ready.")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(boxRow("  BREW PHASE  ──  orders appear automatically over time.")).append("\n");
        t.append(boxRow("                  claim one before a teammate does, then brew")).append("\n");
        t.append(boxRow("                  it with the right ingredients to earn gold.")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(divider()).append("\n");
        t.append(boxRow(TerminalColors.YELLOW + "  COMMANDS" + TerminalColors.RESET)).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(boxRow("  join <code>         enter or create a lobby")).append("\n");
        t.append(boxRow("  shop <item> [qty]   buy ingredients (e.g. shop water 5)")).append("\n");
        t.append(boxRow("  start               begin the round  (host only)")).append("\n");
        t.append(boxRow("  claim <potion name> lock an order before teammates do")).append("\n");
        t.append(boxRow("  brew <ing1,ing2>    mix ingredients to fill the order")).append("\n");
        t.append(boxRow("  quit                leave")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(bottomBar()).append("\n");
        return t.toString();
    }

    // ── private view builders ─────────────────────────────────────────────────

    private static void buildShopView(StringBuilder sb, GameRoom room, long recipeBookHiddenUntil) {
        sb.append(boxRow(TerminalColors.YELLOW + TerminalColors.BOLD
                + " INGREDIENT SHOP" + TerminalColors.RESET)).append("\n");
        sb.append(emptyRow()).append("\n");
        sb.append(boxRow(TerminalColors.DIM
                + "  item              price   in stock" + TerminalColors.RESET)).append("\n");

        String[][] catalog = {
            {"water",        " 5g"},
            {"dragon scale", "15g"},
            {"fairy dust",   "12g"},
            {"fire pepper",  "10g"},
            {"lunar shard",  "20g"},
            {"void extract", "25g"},
        };

        for (String[] row : catalog) {
            int qty = room.roomPantry.getStock(row[0]);
            String stockStr = qty == 0
                    ? TerminalColors.RED   + "out of stock" + TerminalColors.RESET
                    : TerminalColors.GREEN + qty + " units"  + TerminalColors.RESET;
            sb.append(boxRow(
                    "  " + TerminalColors.CYAN + padRight(row[0], 16) + TerminalColors.RESET
                    + TerminalColors.YELLOW + row[1] + TerminalColors.RESET
                    + "     " + stockStr
            )).append("\n");
        }

        sb.append(divider()).append("\n");
        sb.append(boxRow(TerminalColors.MAGENTA + TerminalColors.BOLD
                + " RECIPE BOOK" + TerminalColors.RESET)).append("\n");
        sb.append(emptyRow()).append("\n");

        if (System.currentTimeMillis() < recipeBookHiddenUntil) {
            sb.append(boxRow(TerminalColors.RED
                    + "  [ You can't read through the soot! ]" + TerminalColors.RESET)).append("\n");
            sb.append(emptyRow()).append("\n");
        } else {
            sb.append(boxRow(TerminalColors.DIM
                    + "  name                 tier  sell    ingredients" + TerminalColors.RESET)).append("\n");
            for (String type : PotionFactory.ALL_TYPES) {
                Potion p = PotionFactory.create(type);
                if (p == null) continue;
                sb.append(boxRow(
                        "  " + padRight(p.getName(), 21)
                        + " T" + p.getTier()
                        + "   " + padRight(p.getPrice() + "g", 5)
                        + "   " + TerminalColors.DIM + p.getRecipeString() + TerminalColors.RESET
                )).append("\n");
            }
        }
        sb.append(emptyRow()).append("\n");
    }

    private static void buildRoundView(StringBuilder sb, GameRoom room, long recipeBookHiddenUntil) {
        java.util.List<OrderTicket> snap = room.orders.getSnapshot();
        int total   = snap.size();
        int display = Math.min(total, MAX_ORDERS_SHOWN);

        sb.append(boxRow(TerminalColors.RED + TerminalColors.BOLD
                + " ACTIVE ORDERS (" + total + ")" + TerminalColors.RESET)).append("\n");
        sb.append(emptyRow()).append("\n");

        if (snap.isEmpty()) {
            sb.append(boxRow("  no orders yet... they're coming.")).append("\n");
        } else {
            // Only render the first MAX_ORDERS_SHOWN entries
            for (int i = 0; i < display; i++) {
                OrderTicket tk       = snap.get(i);
                String      claimed  = tk.getClaimedBy();
                String      status   = claimed == null
                        ? TerminalColors.GREEN  + "[open]           " + TerminalColors.RESET
                        : TerminalColors.YELLOW + "[" + padRight(claimed, 15) + "]" + TerminalColors.RESET;

                String recipeStr = (System.currentTimeMillis() < recipeBookHiddenUntil)
                        ? "[ soot-covered ]"
                        : tk.getPotion().getRecipeString();

                sb.append(boxRow(
                        " " + status
                        + " " + TerminalColors.CYAN + padRight(tk.getPotion().getName(), 16) + TerminalColors.RESET
                        + " " + TerminalColors.DIM  + recipeStr + TerminalColors.RESET
                )).append("\n");
            }

            // If there are more orders than can be shown, say so instead of overflowing
            if (total > MAX_ORDERS_SHOWN) {
                int hidden = total - MAX_ORDERS_SHOWN;
                sb.append(boxRow(
                        TerminalColors.DIM + "  ... and " + hidden + " more order"
                        + (hidden == 1 ? "" : "s") + " (complete current ones to see them)"
                        + TerminalColors.RESET
                )).append("\n");
            }
        }

        // Pad to a fixed height so the box doesn't shrink/expand between ticks,
        // which would cause the cursor to jump around
        int rows = Math.min(total, MAX_ORDERS_SHOWN) + (total > MAX_ORDERS_SHOWN ? 1 : 0);
        for (int i = rows; i < MAX_ORDERS_SHOWN + 1; i++) sb.append(emptyRow()).append("\n");
    }

    // ── box-drawing primitives ────────────────────────────────────────────────

    public static String topBar(String title) {
        int sides = BOX_W - title.length() - 2;
        int left  = sides / 2;
        int right = sides - left;
        return TerminalColors.CYAN + TerminalColors.BOLD
                + "╔" + "═".repeat(left) + " " + title + " " + "═".repeat(right) + "╗"
                + TerminalColors.RESET;
    }

    public static String divider() {
        return TerminalColors.CYAN + "╠" + "═".repeat(BOX_W) + "╣" + TerminalColors.RESET;
    }

    public static String bottomBar() {
        return TerminalColors.CYAN + "╚" + "═".repeat(BOX_W) + "╝" + TerminalColors.RESET;
    }

    public static String boxRow(String content) {
        int visLen = TerminalColors.stripAnsi(content).length();
        int pad    = Math.max(0, CONTENT_W - visLen);
        return TerminalColors.CYAN + "║ " + TerminalColors.RESET
                + content + " ".repeat(pad)
                + TerminalColors.CYAN + " ║" + TerminalColors.RESET;
    }

    public static String emptyRow() { return boxRow(""); }

    public static String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }
}