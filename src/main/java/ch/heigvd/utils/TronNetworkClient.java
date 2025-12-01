package ch.heigvd.utils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TronNetworkClient implements Runnable {

    private static final String END_OF_LINE = "\n";
    private final String host;
    private final int port;
    private final String playerName;
    private final TronMessageListener listener;

    private BufferedWriter out;
    private volatile boolean running = true;

    public TronNetworkClient(String host, int port, String playerName, TronMessageListener listener) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
        this.listener = listener;
    }

    public void start() {
        Thread t = new Thread(this, "NetworkClientThread");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
    }

    // Envoi de messages génériques
    public void send(String message) {
        try {
            if (out != null) {
                synchronized (out) {
                    out.write(message + END_OF_LINE);
                    out.flush();
                }
            }
        } catch (IOException e) {
            listener.onError(0, "Erreur d'envoi : " + e.getMessage());
        }
    }

    // Helpers pour les commandes courantes
    public void sendReady() { send("READY"); }
    public void sendInput(String dir) { send("INPUT " + dir); }

    @Override
    public void run() {
        try (
                Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            this.out = writer;
            send("HELLO " + playerName);

            String line;
            while (running && (line = in.readLine()) != null) {
                // Notification brute (pour CLI)
                listener.onRawMessage(line);

                // Parsing pour GUI
                if (line.startsWith("GAME_START")) handleGameStart(line);
                else if (line.startsWith("STATE")) handleState(line);
                else if (line.startsWith("GAME_END")) handleGameEnd(line);
                else if (line.startsWith("ERROR")) parseError(line);
            }
        } catch (IOException e) {
            listener.onError(0, "Déconnecté du serveur : " + e.getMessage());
        }
    }

    private void handleGameStart(String line) {
        String[] parts = line.split(" ");
        if (parts.length < 10) return;
        try {
            int p1x = Integer.parseInt(parts[3]);
            int p1y = Integer.parseInt(parts[4]);
            int p2x = Integer.parseInt(parts[7]);
            int p2y = Integer.parseInt(parts[8]);
            listener.onState("RUNNING", p1x, p1y, true, p2x, p2y, true, new ArrayList<>());
        } catch (NumberFormatException ignored) {}
    }

    private void handleState(String line) {
        try {
            String[] parts = line.split(" ", 6);
            if (parts.length != 6) return;

            String phase = parts[3];
            String[] players = parts[4].split(",");
            String[] trailsPart = parts[5].split(",");

            if (players.length < 2) return;
            String[] p1 = players[0].split(":");
            String[] p2 = players[1].split(":");

            List<Point> trails = new ArrayList<>();
            if (!parts[5].equals("-")) {
                for (String t : trailsPart) {
                    String[] xy = t.split(":");
                    trails.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
                }
            }

            listener.onState(phase,
                    Integer.parseInt(p1[1]), Integer.parseInt(p1[2]), "1".equals(p1[4]),
                    Integer.parseInt(p2[1]), Integer.parseInt(p2[2]), "1".equals(p2[4]),
                    trails);
        } catch (Exception e) {
            // Parsing error suppression
        }
    }

    private void handleGameEnd(String line) {
        String[] parts = line.split(" ");
        if (parts.length >= 3) listener.onGameEnd(parts[1], parts[2]);
    }

    private void parseError(String line) {
        String[] parts = line.split(" ", 3);
        int code = 0;
        String msg = "";
        if (parts.length > 1) try { code = Integer.parseInt(parts[1]); } catch(Exception e){}
        if (parts.length > 2) msg = parts[2];
        listener.onError(code, msg);
    }
}