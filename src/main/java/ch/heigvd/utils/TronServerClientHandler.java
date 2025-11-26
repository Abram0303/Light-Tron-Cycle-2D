package ch.heigvd.utils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TronServerClientHandler implements Runnable {

    private final Socket socket;
    private final int tickMillis;
    private static final String SERVER_VERSION = "1.0";

    public TronServerClientHandler(Socket socket, int tickMillis) {
        this.socket = socket;
        this.tickMillis = tickMillis;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                )
        ) {
            String line = in.readLine();
            if (line == null) {
                System.out.println("Client déconnecté immédiatement.");
                return;
            }

            System.out.println("Reçu du client: " + line);

            String[] parts = line.split(" ");
            if (parts.length < 3 || !"HELLO".equals(parts[0])) {
                sendError(out, 2, "Expected HELLO <version> <playerName>");
                return;
            }

            String clientVersion = parts[1];
            String playerName = parts[2];

            if (!SERVER_VERSION.equals(clientVersion)) {
                sendError(out, 1, "Unsupported version " + clientVersion);
                return;
            }

            String playerId = "P-" + UUID.randomUUID();
            String matchId = "M-1";
            int width = 40;
            int height = 30;

            String welcome = String.format(
                    "WELCOME %s %s %s %d %d %d",
                    SERVER_VERSION,
                    playerId,
                    matchId,
                    width,
                    height,
                    tickMillis
            );

            sendLine(out, welcome);
            System.out.println("Envoyé à " + playerName + " : " + welcome);

            // TODO, gérer READY, GAME_START, loop de jeu, etc.

        } catch (IOException e) {
            System.err.println("Erreur côté client handler : " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) { }
        }
    }

    private void sendLine(BufferedWriter out, String msg) throws IOException {
        out.write(msg);
        out.newLine();
        out.flush();
    }

    private void sendError(BufferedWriter out, int code, String message) throws IOException {
        String line = String.format("ERROR %d %s", code, message);
        sendLine(out, line);
        System.out.println("Envoyé au client : " + line);
    }
}
