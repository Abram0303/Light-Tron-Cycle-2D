package ch.heigvd.utils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TronServerClientHandler implements Runnable {

    private static final String END_OF_LINE = "\n";

    private final TronServer server; // Référence au serveur pour enregistrer les joueurs
    private final Socket clienSocket;
    private String playerName;
    private String playerId;
    private boolean ready = false;

    private BufferedWriter out; // Flux de sortie vers le client (partagé)

    String getPlayerId() {
        return playerId;
    }

    public TronServerClientHandler(TronServer server, Socket clienSocket) {
        this.server = server;
        this.clienSocket = clienSocket;
    }

    public String getPlayer() {
        return playerName;
    }

    @Override
    public void run() {
        try (
                Socket socket = clienSocket;

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                )
        ) {
            this.out = bw; // Obligatoire pour pouvoir l'utiliser dans la méthode sendMessage

            String line = in.readLine();
            if (line == null) {
                System.out.println("Client déconnecté immédiatement.");
                return;
            }

            System.out.println("<< " + line);

            // Traiter le message HELLO
            String[] parts = line.split(" ");
            if (parts.length < 2 || !"HELLO".equals(parts[0])) {
                sendError(out, 2, "Message invalide (attendu : HELLO <playerName>)");
                return;
            }
            playerName = parts[1];

            // Construction et envoi du message WELCOME
            playerId = "P-" + UUID.randomUUID();
            String welcome = String.format("WELCOME %s", playerId);
            sendMessage(welcome);
            System.out.println();

            // Enregistrer le joueur nouvellement connecté sur le serveur si besoin (gestion de la file d'attente, etc.)
            // Todo

            // Boucle principale pour lire les messages READY, INPUT, etc.
            while ((line = in.readLine()) != null) {
                System.out.println("<< " + line);
                handleCommand(line, out, playerName);
            }

            System.out.println("Client déconnecté : " + socket.getRemoteSocketAddress());

        } catch (IOException e) {
            System.err.println("Erreur côté client handler : " + e.getMessage());
        }
    }

    // Gère les commandes reçues du client
    private void handleCommand(String line, BufferedWriter out, String playerName) throws IOException {
        String[] parts = line.trim().split(" ");
        if (parts.length == 0) {
            sendError(out, 2, "message vide");
            return;
        }

        String command = parts[0];

        switch (command) {
            case "READY" :
                handleReady(playerName);
                break;
            case "INPUT" :
                handleInput(parts, out, playerName);
                break;
            default :
                sendError(out, 2, "commande inconnue : " + command);
                break;
        }
    }

    // Gère la commande READY
    private void handleReady(String playerName) throws IOException {
        if (ready) {
            sendError(out, 3, "READY déjà envoyé");
            return;
        }
        // Marquer le joueur comme prêt
        ready = true;
        System.out.println("Joueur " + playerName + " est READY");
        server.registerPlayer(this);
    }

    // Gère la commande INPUT
    private void handleInput(String[] parts, BufferedWriter out, String playerName) throws IOException {
        if (parts.length != 2) {
            sendError(out, 2, "usage: INPUT <direction>");
            return;
        }

        String direction = parts[1];

        if (!direction.equals("UP") &&
                !direction.equals("DOWN") &&
                !direction.equals("LEFT") &&
                !direction.equals("RIGHT")) {
            sendError(out, 2, "direction invalide : " + direction);
            return;
        }

        System.out.println("INPUT reçu de " + playerName + " : " + direction);

        // Plus tard : enregistrer cette direction dans l'état du jeu pour qu'elle soit appliquée au prochain tick.
    }

    public void sendMessage(String message) throws IOException {
        out.write(message + END_OF_LINE);
        out.flush();
        System.out.println(">> " + message);
    }

    private void sendError(BufferedWriter out, int code, String message) throws IOException {
        String error = "ERROR " + code + " " + message;
        sendMessage(error);
    }
}
