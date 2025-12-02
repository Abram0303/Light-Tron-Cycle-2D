package ch.heigvd.commands;

import ch.heigvd.gui.TronClientFX;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "gui", description = "Lance l'interface graphique.")
public class GuiCommand implements Runnable {

    @Option(names = {"-h", "--host"}, description = "Adresse IP du serveur", defaultValue = "localhost")
    private String host;

    @Option(names = {"-p", "--port"}, description = "Port du serveur", defaultValue = "2222")
    private int port;

    @Override
    public void run() {
        // On passe les paramètres à la classe JavaFX via des variables statiques
        TronClientFX.serverHost = host;
        TronClientFX.serverPort = port;

        TronClientFX.main(new String[0]);
    }
}