package ch.heigvd.commands;

import ch.heigvd.utils.Point;
import ch.heigvd.utils.TronMessageListener;
import ch.heigvd.utils.TronNetworkClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Scanner;

@Command(name = "client", description = "Démarre un client Light-Tron en mode texte.")
public class ClientCommand implements Runnable {

    @Option(names = {"-h", "--host"}, defaultValue = "localhost")
    private String host;

    @Option(names = {"-p", "--port"}, defaultValue = "2222")
    private int port;

    @Option(names = {"-n", "--name"}, required = true)
    private String playerName;

    @Override
    public void run() {
        System.out.println("Démarrage du client Console...");

        TronNetworkClient client = new TronNetworkClient(host, port, playerName, new TronMessageListener() {
            @Override
            public void onRawMessage(String msg) {
                System.out.println("<< " + msg);
            }
            // Correction ici : acceptation de t1 et t2
            @Override
            public void onState(String p, int x1, int y1, boolean a1, int x2, int y2, boolean a2, List<Point> t1, List<Point> t2) {
                // Rien à faire en mode console graphique
            }
            @Override
            public void onGameEnd(String reason, String winnerId) {
                System.out.println("FIN DE PARTIE: " + reason + " (Vainqueur: " + winnerId + ")");
            }
            @Override
            public void onError(int code, String message) {
                System.err.println("ERREUR " + code + ": " + message);
            }
        });

        client.start();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if ("quit".equalsIgnoreCase(line)) {
                client.stop();
                break;
            }
            client.send(line);
        }
    }
}