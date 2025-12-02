package ch.heigvd.commands;

import ch.heigvd.utils.Point;
import ch.heigvd.utils.TronMessageListener;
import ch.heigvd.utils.TronNetworkClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Scanner;

@Command(name = "client", description = "Démarre un client Light-Tron en mode texte (REPL).")
public class ClientCommand implements Runnable {

    @Option(names = {"-h", "--host"}, defaultValue = "localhost")
    private String host;

    @Option(names = {"-p", "--port"}, defaultValue = "2222")
    private int port;

    @Option(names = {"-n", "--name"}, required = true)
    private String playerName;

    private TronNetworkClient client;

    @Override
    public void run() {
        System.out.println("--- Light Tron CLI ---");
        System.out.println("Connexion à " + host + ":" + port + " en tant que " + playerName + "...");
        System.out.println("Tapez 'help' pour voir les commandes.");

        // Initialisation Client
        client = new TronNetworkClient(host, port, playerName, new TronMessageListener() {
            @Override
            public void onRawMessage(String msg) {
                if (!msg.startsWith("STATE")) {
                    printAsyncMessage("[SERVER] " + msg);
                }
            }

            @Override
            public void onState(String p, int x1, int y1, boolean a1, int x2, int y2, boolean a2, List<Point> t1, List<Point> t2) {}

            @Override
            public void onGameEnd(String reason, String winnerId) {
                printAsyncMessage("\n>>> FIN DE PARTIE : " + reason + " | Vainqueur : " + winnerId);
            }

            @Override
            public void onError(int code, String message) {
                printAsyncMessage(">>> ERREUR " + code + ": " + message);
                if (code == 4) {
                    System.exit(0);
                }
            }
        });

        client.start();

        // Boucle REPL (Read-Eval-Print Loop)
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");

            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;

            handleUserCommand(line);
        }
    }

    /**
     * Traite la commande saisie par l'utilisateur
     */
    private void handleUserCommand(String line) {
        String[] tokens = line.split("\\s+");
        String cmd = tokens[0].toLowerCase();

        switch (cmd) {
            case "help", "?" -> printHelp();

            case "quit", "exit" -> {
                System.out.println("Déconnexion...");
                client.stop();
                System.exit(0);
            }

            case "ready" -> {
                System.out.println("Envoi de READY au serveur...");
                client.sendReady();
            }

            // Mappage des directions
            case "up", "w", "z" -> client.sendInput("UP");
            case "down", "s" -> client.sendInput("DOWN");
            case "left", "a", "q" -> client.sendInput("LEFT");
            case "right", "d" -> client.sendInput("RIGHT");

            default -> System.out.println("Commande inconnue. Tapez 'help'.");
        }
    }

    /**
     * Affiche un message venant du serveur sans casser le prompt de l'utilisateur.
     * Utilise \r pour revenir au début de la ligne.
     */
    private void printAsyncMessage(String msg) {
        System.out.println("\r" + msg);
        System.out.print("> ");
    }

    private void printHelp() {
        System.out.println(
            """
            Commandes disponibles :
            -----------------------
            ready          : Se déclarer prêt à jouer
            up (z/w)       : Aller en HAUT
            down (s)       : Aller en BAS
            left (q/a)     : Aller à GAUCHE
            right (d)      : Aller à DROITE
            quit           : Quitter le jeu
            help           : Afficher ce menu
            """
        );
    }
}