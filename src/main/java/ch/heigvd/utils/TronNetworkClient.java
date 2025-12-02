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

    /**
     * Lance le thread réseau en arrière-plan (daemon) pour écouter le serveur.
     */
    public void start() {
        Thread t = new Thread(this, "NetworkClientThread");
        t.setDaemon(true);
        t.start();
    }

    public void stop() { running = false; }

    /**
     * Envoie un message brut au serveur de manière thread-safe (synchronized).
     */
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

    public void sendReady() { send("READY"); }
    public void sendInput(String dir) { send("INPUT " + dir); }

    /**
     * Boucle principale : connecte le socket, effectue le handshake et écoute les messages entrants.
     */
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
                listener.onRawMessage(line);

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
            listener.onState("RUNNING", p1x, p1y, true, p2x, p2y, true, new ArrayList<>(), new ArrayList<>());
        } catch (NumberFormatException ignored) {}
    }

    /**
     * Parse le message STATE complet (phase, joueurs, et les deux listes de traînées distinctes).
     */
    private void handleState(String line) {
        try {
            String[] parts = line.split(" ", 7);
            if (parts.length != 7) return;

            String phase = parts[3];
            String[] players = parts[4].split(",");

            if (players.length < 2) return;
            String[] p1 = players[0].split(":");
            String[] p2 = players[1].split(":");

            // Parsing des deux listes
            List<Point> t1 = parseTrails(parts[5]);
            List<Point> t2 = parseTrails(parts[6]);

            listener.onState(phase,
                    Integer.parseInt(p1[1]), Integer.parseInt(p1[2]), "1".equals(p1[4]),
                    Integer.parseInt(p2[1]), Integer.parseInt(p2[2]), "1".equals(p2[4]),
                    t1, t2);
        } catch (Exception e) {
            // Ignorer les erreurs de parsing partielles
        }
    }

    /**
     * Convertit une chaîne de coordonnées compressée ("x:y,x:y") en une liste d'objets Point.
     */
    private List<Point> parseTrails(String data) {
        List<Point> list = new ArrayList<>();
        if (!data.equals("-") && !data.isEmpty()) {
            for (String t : data.split(",")) {
                String[] xy = t.split(":");
                list.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }
        }
        return list;
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