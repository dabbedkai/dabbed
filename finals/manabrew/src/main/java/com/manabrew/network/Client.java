package com.manabrew.network;

import java.io.*;
import java.net.*;

public class Client {

    // Shared buffer between the listener thread (which triggers reprints) and
    // the main thread (which appends characters). All access is synchronized.
    private static final StringBuilder inputBuffer = new StringBuilder();

    public static void main(String[] args) {
        try (
                Socket cSocket = new Socket("localhost", 8080);
                BufferedReader fromServer = new BufferedReader(
                        new InputStreamReader(cSocket.getInputStream()));
                PrintWriter toServer = new PrintWriter(cSocket.getOutputStream(), true)) {
            // ── listener thread ───────────────────────────────────────────────
            // Prints every line from the server. After the server sends the
            // prompt line ("> ") the screen has just been redrawn, so we
            // immediately reprint whatever the player had typed so far.
            Thread listener = new Thread(() -> {
                try {
                    String line;
                    while ((line = fromServer.readLine()) != null) {
                        System.out.println(line);

                        // The server ends every redraw with a "> " prompt line.
                        // Reprint the in-progress input so it's not lost.
                        if (line.contains("> ")) {
                            synchronized (inputBuffer) {
                                if (inputBuffer.length() > 0) {
                                    // \r returns to start of line, then we
                                    // overwrite with the prompt + buffer
                                    System.out.print(
                                            TerminalColors.GREEN + "> " + TerminalColors.RESET
                                                    + inputBuffer.toString());
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println(TerminalColors.RED
                            + "\n[disconnected] server closed the connection."
                            + TerminalColors.RESET);
                    System.exit(0);
                }
            });
            listener.setDaemon(true);
            listener.start();

            // ── main input loop (character-by-character) ──────────────────────
            // Read raw bytes so we can keep our own buffer that survives redraws.
            DataInputStream kb = new DataInputStream(System.in);

            while (true) {
                int b = kb.read();
                if (b == -1)
                    break;

                char c = (char) b;

                if (c == '\n' || c == '\r') {
                    // Enter pressed — send the line only if non-empty
                    String line;
                    synchronized (inputBuffer) {
                        line = inputBuffer.toString().trim();
                        inputBuffer.setLength(0);
                    }
                    System.out.println();
                    if (line.isEmpty())
                        continue; // don't send blank lines
                    toServer.println(line);
                    if (line.equalsIgnoreCase("quit")) {
                        System.out.println("closing client...");
                        break;
                    }

                } else if (c == '\b' || b == 127) {
                    // Backspace — remove last character from buffer and terminal
                    synchronized (inputBuffer) {
                        if (inputBuffer.length() > 0) {
                            inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                            // \b moves cursor back, space erases, \b moves back again
                            System.out.print("\b \b");
                        }
                    }

                } else if (c >= 32) {
                    // Printable character — append and echo
                    synchronized (inputBuffer) {
                        inputBuffer.append(c);
                    }
                    System.out.print(c);
                }
            }

        } catch (Exception e) {
            System.out.println("couldn't connect. is Server.java running on port 8080?");
        }
    }
}