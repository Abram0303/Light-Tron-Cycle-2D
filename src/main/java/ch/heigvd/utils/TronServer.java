package ch.heigvd.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TronServer {

    private static final String END_OF_LINE = "\n";

    // Liste des joueurs prêts à jouer (thread-safe)
    private final CopyOnWriteArrayList<TronServerClientHandler> readyPlayers = new CopyOnWriteArrayList<>();

    // Générateur d'IDs uniques pour les joueurs (thread-safe)
    private static final AtomicInteger nextPlayerId = new AtomicInteger(1);

    private final int port;
    private final int tickMillis; // Durée d'un tick en millisecondes
    private final ExecutorService clientPool; // Pool de threads pour gérer les clients
    private final ExecutorService gamePool; // Pool de threads pour gérer les parties

    public TronServer(int port, int tickMillis) {
        this.port = port;
        this.tickMillis = tickMillis;
        this.clientPool = Executors.newFixedThreadPool(2); // Maximum 2 joueurs simultanés
        this.gamePool = Executors.newSingleThreadExecutor(); // Une partie à la fois
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("Serveur lancé sur le port %d%n", port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouveau client : " + clientSocket.getRemoteSocketAddress());

                // Gérer les clients dans des threads séparés
                TronServerClientHandler handler = new TronServerClientHandler(this, clientSocket);
                clientPool.submit(handler);
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    // Méthode pour enregistrer un joueur prêt dans la liste
    public void registerPlayer(TronServerClientHandler handler) {
        readyPlayers.add(handler);
        System.out.println("Nombre de joueurs prêts : " + readyPlayers.size());
        System.out.println();

        if (readyPlayers.size() == 2) {
            System.out.println("Deux joueurs prêts. Démarrage de la partie !");

            String matchId = "M1";

            TronServerClientHandler p1 = readyPlayers.get(0);
            TronServerClientHandler p2 = readyPlayers.get(1);

            // Enlever les deux joueurs de la liste des joueurs prêts
            readyPlayers.remove(p1);
            readyPlayers.remove(p2);

            TronGame game = new TronGame(matchId, p1, p2, tickMillis);
            gamePool.submit(game);

        }
    }

    public int generatePlayerId() {
        return nextPlayerId.getAndIncrement();
    }
}
