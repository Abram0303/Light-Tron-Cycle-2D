package ch.heigvd.utils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TronClient {

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

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );

                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                );

                Scanner stdin = new Scanner(System.in)
        ) {
            System.out.printf("Connecté à %s:%d%n", host, port);

            // HELLO
            String hello = "HELLO 1.0 " + playerName;
            out.write(hello);
            out.newLine();
            out.flush();

            System.out.println(">> " + hello);

            // Lire la réponse du serveur
            String response = in.readLine();
            if (response == null) {
                System.out.println("Connexion fermée par le serveur.");
                return;
            }
            System.out.println("<< " + response);

        } catch (IOException e) {
            System.err.println("Erreur client : " + e.getMessage());
        }
    }
}
