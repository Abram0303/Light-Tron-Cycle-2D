package ch.heigvd.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;

public class TronServer {

    private static final String END_OF_LINE = "\n";

    // Liste des joueurs prêts à jouer (thread-safe)
    private final CopyOnWriteArrayList<TronServerClientHandler> readyPlayers = new CopyOnWriteArrayList<>();

    private final int port;
    private final int tickMillis; // Durée d'un tick en millisecondes
    private final ExecutorService clientPool;

    public TronServer(int port, int tickMillis) {
        this.port = port;
        this.tickMillis = tickMillis;
        this.clientPool = Executors.newFixedThreadPool(2); // Maximum 2 joueurs simultanés
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

            // Positions et directions initiales
            int p1x = 5,  p1y = 10;
            int p2x = 35, p2y = 10;
            String p1dir = "RIGHT";
            String p2dir = "LEFT";

            String gameStart = String.format(
                    "GAME_START %s %s %d %d %s %s %d %d %s",
                    matchId,
                    p1.getPlayerId(), p1x, p1y, p1dir,
                    p2.getPlayerId(), p2x, p2y, p2dir
            );

            try {
                p1.sendMessage(gameStart + END_OF_LINE);
                p2.sendMessage(gameStart + END_OF_LINE);
            } catch (IOException e) {
                System.err.println("Erreur lors de l'envoi de GAME_START : " + e.getMessage());
            }

        }
    }
}
