package com.manabrew.network;

import com.manabrew.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    /** Session-wide counters written to shift_logs.json on shutdown. */
    public static final AtomicInteger totalGold        = new AtomicInteger(0);
    public static final AtomicInteger potionsDelivered = new AtomicInteger(0);

    /** All active lobbies keyed by their room code. */
    private static final Map<String, GameRoom> activeRooms = new ConcurrentHashMap<>();

    /**
     * Returns an existing room or creates a new one.
     * Synchronized so two clients hitting the same code simultaneously
     * don't race-create two separate rooms.
     */
    public static synchronized GameRoom getOrCreateRoom(String code) {
        if (!activeRooms.containsKey(code)) {
            activeRooms.put(code, new GameRoom(code));
            System.out.println(TerminalColors.YELLOW + "new lobby: [" + code + "]"
                    + TerminalColors.RESET);
        }
        return activeRooms.get(code);
    }

    public static void main(String[] args) {
        setupSaveHook();

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println(TerminalColors.CYAN
                    + "ManaBrew server is online on port 8080." + TerminalColors.RESET);
            System.out.println("waiting for players...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println(TerminalColors.RED
                    + "port 8080 is already in use. kill whatever's on it and retry."
                    + TerminalColors.RESET);
        }
    }

    /** Writes shift analytics to shift_logs.json when the process exits. */
    private static void setupSaveHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Gson gson    = new GsonBuilder().setPrettyPrinting().create();
            File logFile = new File("shift_logs.json");
            ArrayList<ShiftLog> history = new ArrayList<>();

            if (logFile.exists()) {
                try (Reader r = new FileReader(logFile)) {
                    ShiftLog[] old = gson.fromJson(r, ShiftLog[].class);
                    if (old != null) history.addAll(Arrays.asList(old));
                } catch (Exception e) {
                    System.out.println("existing shift_logs.json was unreadable, starting fresh.");
                }
            }

            history.add(new ShiftLog(
                    LocalDateTime.now().toString(),
                    potionsDelivered.get(),
                    totalGold.get()
            ));

            try (Writer w = new FileWriter(logFile)) {
                gson.toJson(history, w);
                System.out.println(TerminalColors.GREEN
                        + "[saved] shift analytics written to shift_logs.json"
                        + TerminalColors.RESET);
            } catch (IOException e) {
                System.out.println(TerminalColors.RED
                        + "[error] could not write shift_logs.json" + TerminalColors.RESET);
            }
        }));
    }
}
