package ch.heigvd.utils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TronClient {

    private static final String END_OF_LINE = "\n";

    private final String host;
    private final int port;
    private final String playerName;

    public TronClient(String host, int port, String playerName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
    }

    public void start() {
        try (
                Socket socket = new Socket(host, port);

                // Flux d'entrée bufferisé pour lire les données du serveur
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );

                // Flux de sortie bufferisé pour envoyer des données au serveur
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                );

                // Préparation de la lecture au clavier
                Scanner stdin = new Scanner(System.in)
        ) {
            System.out.printf("Connecté à %s:%d%n", host, port);

            // Commande HELLO
            String hello = "HELLO " + playerName;
            out.write(hello + END_OF_LINE);
            out.flush();

            System.out.println(">> " + hello);

            // Attendre et lire la réponse du serveur
            String response = in.readLine();
            if (response == null) {
                System.out.println("Connexion fermée par le serveur.");
                return;
            }
            System.out.println("<< " + response);

            if (response.startsWith("ERROR")) {
                System.out.println("Le serveur a refusé la connexion. Arrêt du client.");
                return;
            }

            System.out.println("Connexion acceptée. Vous pouvez maintenant envoyer des commandes.");

            // Thread dédié à l'écoute des messages du serveur
            Thread listener = new Thread(() -> {
                try {
                    // Attendre et lire la réponse du serveur
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("<< " + line);
                    }
                    System.out.println("Connexion fermée par le serveur.");
                } catch (IOException e) {
                    System.err.println("Erreur de réception depuis le serveur : " + e.getMessage());
                }
            });
            listener.setDaemon(true); // Permet de terminer le thread à la fermeture du programme
            listener.start(); // Démarrer le thread d'écoute

            // Thread principal dédié à l'envoi des commandes au serveur
            while (true) {
                System.out.print("> ");
                if (!stdin.hasNextLine()) {
                    break;
                }

                // Attendre et lire la commande entrée par l'utilisateur
                String input = stdin.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equalsIgnoreCase("quit")) {
                    System.out.println("Fermeture du client.");
                    break;
                }

                out.write(input + END_OF_LINE);
                out.flush();
                System.out.println(">> " + input);
            }

        } catch (IOException e) {
            System.err.println("Erreur client : " + e.getMessage());
        }
    }
}
