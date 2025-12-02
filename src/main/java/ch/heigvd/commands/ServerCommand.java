package ch.heigvd.commands;

import ch.heigvd.utils.TronServer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "server",
        description = "Démarre le serveur Light-Tron."
)
public class ServerCommand implements Runnable {

    @Option(
            names = {"-p", "--port"},
            description = "Port TCP d'écoute (par défaut : 2222)",
            defaultValue = "2222"
    )
    private int port;

    @Option(
            names = {"-t", "--tick-millis"},
            description = "Durée d'un tick de jeu en millisecondes (par défaut : 150)",
            defaultValue = "150"
    )
    private int tickMillis;

    @Override
    public void run() {
        System.out.printf("Démarrage du serveur sur le port %d (tick=%d ms)...%n", port, tickMillis);
        TronServer server = new TronServer(port, tickMillis);
        server.start();
    }
}
