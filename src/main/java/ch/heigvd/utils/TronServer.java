package ch.heigvd.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TronServer {

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
                clientPool.submit(new TronServerClientHandler(clientSocket, tickMillis));
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            clientPool.shutdown();
        }
    }
}
