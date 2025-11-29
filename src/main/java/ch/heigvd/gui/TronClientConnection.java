package ch.heigvd.gui;

import ch.heigvd.utils.Point;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class TronClientConnection {

    private static final String END_OF_LINE = "\n";

    private final String host;
    private final int port;
    private final String playerName;
    private final TronMessageListener listener;

    private BufferedWriter out;

    public TronClientConnection(String host, int port, String playerName,
                                TronMessageListener listener) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
        this.listener = listener;
    }

    public void start() {
        Thread t = new Thread(this::run, "TronClientConnection");
        t.setDaemon(true);
        t.start();
    }

    private void run() {
        try (
                Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            this.out = writer;

            // HELLO
            sendLine("HELLO " + playerName);

            String response = in.readLine();
            if (response == null) {
                listener.onError(0, "Connexion fermée par le serveur");
                return;
            }

            if (!response.startsWith("WELCOME")) {
                // On suppose une ERROR
                parseError(response);
                return;
            }

            System.out.println("GUI a envoyé READY");
            // Auto-READY
            sendLine("READY");

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("GAME_START")) {
                    handleGameStart(line);
                } else if (line.startsWith("STATE")) {
                    handleState(line);
                } else if (line.startsWith("GAME_END")) {
                    handleGameEnd(line);
                } else if (line.startsWith("ERROR")) {
                    parseError(line);
                }
            }

            listener.onError(0, "Connexion fermée par le serveur");

        } catch (IOException e) {
            listener.onError(0, "Erreur réseau : " + e.getMessage());
        }
    }

    private void sendLine(String msg) throws IOException {
        synchronized (this) {
            if (out == null) {
                throw new IOException("Flux sortie non initialisé");
            }
            out.write(msg + END_OF_LINE);
            out.flush();
        }
    }

    public void sendInput(String direction) {
        try {
            sendLine("INPUT " + direction);
        } catch (IOException e) {
            listener.onError(0, "Erreur lors de l'envoi INPUT : " + e.getMessage());
        }
    }

    private void handleGameStart(String line) {
        // GAME_START matchId p1Id p1x p1y p1dir p2Id p2x p2y p2dir
        String[] parts = line.split(" ");
        if (parts.length != 10) {
            System.out.println("GAME_START invalide : " + line);
            return;
        }

        int p1x = Integer.parseInt(parts[3]);
        int p1y = Integer.parseInt(parts[4]);
        int p2x = Integer.parseInt(parts[7]);
        int p2y = Integer.parseInt(parts[8]);

        List<Point> trails = new ArrayList<>();
        listener.onState("RUNNING", p1x, p1y, true, p2x, p2y, true, trails);
    }


    private void handleState(String line) {
        // STATE matchId tick phase players trails
        String[] parts = line.split(" ", 6);
        if (parts.length != 6) {
            System.out.println("STATE invalide : " + line);
            return;
        }

        String phase       = parts[3];
        String playersPart = parts[4];
        String trailsPart  = parts[5];

        String[] players = playersPart.split(",");
        if (players.length != 2) {
            System.out.println("Champ players invalide : " + playersPart);
            return;
        }

        // playerId:x:y:dir:alive
        String[] p1Fields = players[0].split(":");
        String[] p2Fields = players[1].split(":");
        if (p1Fields.length != 5 || p2Fields.length != 5) {
            System.out.println("Format player invalide : " + playersPart);
            return;
        }

        int p1x = Integer.parseInt(p1Fields[1]);
        int p1y = Integer.parseInt(p1Fields[2]);
        boolean p1Alive = "1".equals(p1Fields[4]);

        int p2x = Integer.parseInt(p2Fields[1]);
        int p2y = Integer.parseInt(p2Fields[2]);
        boolean p2Alive = "1".equals(p2Fields[4]);

        List<Point> trails = new ArrayList<>();
        if (!"-".equals(trailsPart)) {
            String[] cells = trailsPart.split(",");
            for (String cell : cells) {
                String[] xy = cell.split(":");
                if (xy.length == 2) {
                    int x = Integer.parseInt(xy[0]);
                    int y = Integer.parseInt(xy[1]);
                    trails.add(new Point(x, y));
                }
            }
        }

        listener.onState(phase, p1x, p1y, p1Alive, p2x, p2y, p2Alive, trails);
    }


    private void handleGameEnd(String line) {
        // GAME_END reason winnerId
        String[] parts = line.split(" ");
        if (parts.length != 3) {
            return;
        }
        String reason = parts[1];
        String winnerId = parts[2];
        listener.onGameEnd(reason, winnerId);
    }

    private void parseError(String line) {
        // ERROR <code> <message...>
        String[] parts = line.split(" ", 3);
        if (parts.length < 2) {
            listener.onError(0, "ERROR invalide : " + line);
            return;
        }
        int code;
        try {
            code = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            code = 0;
        }
        String msg = parts.length == 3 ? parts[2] : "";
        listener.onError(code, msg);
    }
}

