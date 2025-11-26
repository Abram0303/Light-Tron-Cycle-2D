package ch.heigvd.commands;

import ch.heigvd.utils.TronClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "client",
        description = "Démarre un client Light-Tron."
)
public class ClientCommand implements Runnable {

    @Option(
            names = {"-h", "--host"},
            description = "Hôte du serveur (par défaut : localhost)",
            defaultValue = "localhost"
    )
    private String host;

    @Option(
            names = {"-p", "--port"},
            description = "Port du serveur (par défaut : 2222)",
            defaultValue = "2222"
    )
    private int port;

    @Option(
            names = {"-n", "--name"},
            description = "Pseudo du joueur (sans espace)",
            required = true
    )
    private String playerName;

    @Override
    public void run() {
        System.out.printf("Connexion au serveur %s:%d avec le pseudo '%s'%n", host, port, playerName);
        TronClient client = new TronClient(host, port, playerName);
        client.start(); // ToDO
    }
}
