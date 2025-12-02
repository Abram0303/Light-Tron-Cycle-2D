package ch.heigvd.utils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TronServerClientHandler implements Runnable {

    private static final String END_OF_LINE = "\n";
    private final TronServer server;
    private final Socket clienSocket;
    private String playerName;
    private String playerId;
    private boolean ready = false;
    private BufferedWriter out;
    private volatile String pendingDirection = null;

    public TronServerClientHandler(TronServer server, Socket clienSocket) {
        this.server = server;
        this.clienSocket = clienSocket;
    }

    public void setPendingDirection(String direction) { this.pendingDirection = direction; }

    /**
     * Récupère la dernière direction demandée par le joueur et la consomme (reset à null).
     * Utilisé par le thread de jeu (TronGame) pour appliquer les inputs.
     */
    public String consumePendingDirection() {
        String d = pendingDirection;
        pendingDirection = null;
        return d;
    }

    public String getPlayerId() { return playerId; }

    /**
     * Boucle principale du thread : gère le handshake (HELLO), écoute les commandes
     * et nettoie les ressources à la déconnexion (finally).
     */
    @Override
    public void run() {
        try (
                Socket socket = clienSocket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            this.out = bw;

            String line = in.readLine();
            if (line == null) return;
            System.out.println("<< " + line);

            String[] parts = line.split(" ");
            if (parts.length < 2 || !"HELLO".equals(parts[0])) {
                sendError(2, "Usage: HELLO <name>");
                return;
            }
            playerName = parts[1];
            playerId = "P" + server.generatePlayerId();
            sendMessage("WELCOME " + playerId);

            while ((line = in.readLine()) != null) {
                System.out.println("<< " + line);
                handleCommand(line);
            }
        } catch (IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            // On retire le client du serveur dès que la boucle s'arrête
            server.removeClient(this);

            // Fermeture de sécurité
            try { clienSocket.close(); } catch (IOException e) {}
        }
    }

    /**
     * Analyse et dirige les commandes reçues (READY, INPUT) vers les méthodes appropriées.
     */
    private void handleCommand(String line) throws IOException {
        String[] parts = line.trim().split(" ");
        if (parts.length == 0) return;
        switch (parts[0]) {
            case "READY" -> handleReady();
            case "INPUT" -> handleInput(parts);
            default -> sendError(2, "Commande inconnue");
        }
    }

    /**
     * Gère la commande READY : marque le joueur comme prêt et notifie le serveur pour lancer la partie.
     */
    private void handleReady() throws IOException {
        if (ready) { sendError(3, "Déjà READY"); return; }
        ready = true;
        server.registerPlayer(this);
    }

    /**
     * Valide et enregistre la direction demandée (INPUT) pour le prochain tick de jeu.
     */
    private void handleInput(String[] parts) throws IOException {
        if (parts.length != 2) { sendError(2, "Usage: INPUT <DIR>"); return; }
        String d = parts[1];
        if (!d.equals("UP") && !d.equals("DOWN") && !d.equals("LEFT") && !d.equals("RIGHT")) {
            sendError(2, "Direction invalide");
            return;
        }
        setPendingDirection(d);
    }

    /**
     * Envoie un message au client de manière thread-safe (synchronized) pour éviter les conflits d'écriture.
     */
    public void sendMessage(String message) throws IOException {
        synchronized (out) {
            out.write(message + END_OF_LINE);
            out.flush();
        }
        System.out.println(">> " + message);
    }

    private void sendError(int code, String message) throws IOException {
        sendMessage("ERROR " + code + " " + message);
    }
}