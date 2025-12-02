package ch.heigvd.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TronServer {

    // Liste des joueurs connectés
    private final CopyOnWriteArrayList<TronServerClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    // Liste des joueurs prêts
    private final CopyOnWriteArrayList<TronServerClientHandler> readyPlayers = new CopyOnWriteArrayList<>();

    private static final AtomicInteger nextPlayerId = new AtomicInteger(1);
    private static final int MAX_PLAYERS = 2;

    private final int port;
    private final int tickMillis;
    private final ExecutorService clientPool;
    private final ExecutorService gamePool;

    public TronServer(int port, int tickMillis) {
        this.port = port;
        this.tickMillis = tickMillis;
        this.clientPool = Executors.newCachedThreadPool();
        this.gamePool = Executors.newSingleThreadExecutor();
    }

    /**
     * Boucle principale : écoute le port TCP, accepte les connexions entrantes
     * et rejette les clients si la capacité maximale est atteinte.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("Serveur lancé sur le port %d (Max %d joueurs)%n", port, MAX_PLAYERS);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Vérification de la capacité avant de créer le thread
                if (connectedClients.size() >= MAX_PLAYERS) {
                    System.out.println("Connexion refusée (Serveur plein): " + clientSocket.getRemoteSocketAddress());
                    sendFullErrorAndClose(clientSocket);
                    continue;
                }

                System.out.println("Nouveau client : " + clientSocket.getRemoteSocketAddress());

                // Ajout et traitement
                TronServerClientHandler handler = new TronServerClientHandler(this, clientSocket);
                connectedClients.add(handler);
                clientPool.submit(handler);
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    /**
     * Envoie le code d'erreur 4 (Serveur plein) et ferme immédiatement la connexion.
     */
    private void sendFullErrorAndClose(Socket socket) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            out.write("ERROR 4 Serveur plein\n");
            out.flush();
            socket.close();
        } catch (IOException ignored) {}
    }

    /**
     * Nettoie les listes en retirant un client qui vient de se déconnecter.
     */
    public void removeClient(TronServerClientHandler handler) {
        connectedClients.remove(handler);
        readyPlayers.remove(handler);

        // Si plus personne n'est connecté, on reset le compteur pour que les prochains soient P1 et P2
        if (connectedClients.isEmpty()) {
            nextPlayerId.set(1);
            System.out.println("Tous les clients sont partis. Reset des IDs à 1.");
        }

        System.out.println("Client déconnecté. Joueurs restants: " + connectedClients.size());
    }

    /**
     * Marque un joueur comme "READY". Si deux joueurs sont prêts,
     * lance une nouvelle instance de jeu (TronGame) dans un thread séparé.
     */
    public void registerPlayer(TronServerClientHandler handler) {
        if (!readyPlayers.contains(handler)) {
            readyPlayers.add(handler);
        }

        System.out.println("Joueur prêt (" + handler.getPlayerId() + "). Total prêts : " + readyPlayers.size());

        if (readyPlayers.size() >= 2) {
            System.out.println("Deux joueurs prêts. Démarrage de la partie !");
            TronServerClientHandler p1 = readyPlayers.get(0);
            TronServerClientHandler p2 = readyPlayers.get(1);

            // On retire les joueurs de la liste prêts pour qu'ils ne relancent pas une partie tout de suite
            readyPlayers.remove(p1);
            readyPlayers.remove(p2);

            TronGame game = new TronGame("M1", p1, p2, tickMillis);
            gamePool.submit(game);
        }
    }

    public int generatePlayerId() {
        return nextPlayerId.getAndIncrement();
    }
}