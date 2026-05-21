package com.manabrew.network;

public class TerminalColors {
    public static final String RESET   = "\u001B[0m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";
    public static final String BOLD    = "\u001B[1m";
    public static final String DIM     = "\u001B[2m";

    /** Wipes the screen and snaps cursor to top-left. */
    public static final String CLEAR          = "\u001B[2J\u001B[H";

    /** Saves the current cursor position. */
    public static final String CURSOR_SAVE    = "\u001B[s";

    /** Restores the cursor to the last saved position. */
    public static final String CURSOR_RESTORE = "\u001B[u";

    /** Erases from cursor to end of line (clears stale prompt characters). */
    public static final String ERASE_LINE     = "\u001B[K";

    /** Strips escape codes so we can measure real visible string length. */
    public static String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*[mHJK]", "");
    }
}