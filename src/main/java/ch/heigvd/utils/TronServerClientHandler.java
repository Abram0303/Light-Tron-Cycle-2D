package ch.heigvd.utils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TronServerClientHandler implements Runnable {

    private static final String END_OF_LINE = "\n";
    private static final String SERVER_VERSION = "1.0";

    private final Socket clienSocket;
    private final int tickMillis;


    public TronServerClientHandler(Socket socket, int tickMillis) {
        this.clienSocket = socket;
        this.tickMillis = tickMillis;
    }

    @Override
    public void run() {
        try (
                Socket socket = clienSocket;

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

            System.out.println("<< " + line);

            // Traiter la commande HELLO
            String[] parts = line.split(" ");
            if (parts.length < 3 || !"HELLO".equals(parts[0])) {
                sendError(out, 2, "Message invalide, attendu : HELLO <version> <playerName>");
                return;
            }

            String clientVersion = parts[1];
            String playerName = parts[2];

            if (!SERVER_VERSION.equals(clientVersion)) {
                sendError(out, 1, "Version non supportée " + clientVersion);
                return;
            }

            // Construction et envoi du message WELCOME
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
            System.out.println(">> " + welcome);

            // TODO, gérer READY, GAME_START, loop de jeu, etc.

        } catch (IOException e) {
            System.err.println("Erreur côté client handler : " + e.getMessage());
        }
    }

    private void sendLine(BufferedWriter out, String msg) throws IOException {
        out.write(msg + END_OF_LINE);
        out.flush();
    }

    private void sendError(BufferedWriter out, int code, String message) throws IOException {
        String error = "ERROR " + code + " " + message;
        sendLine(out, error);
        System.out.println(">> " + error);
    }
}
