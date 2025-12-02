package ch.heigvd;

import ch.heigvd.commands.ClientCommand;
import ch.heigvd.commands.GuiCommand;
import ch.heigvd.commands.ServerCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Point d'entrée principal de l'application CLI.
 * Configure Picocli et définit les sous-commandes disponibles (server, client, gui).
 */
@Command(
        name = "light-tron",
        mixinStandardHelpOptions = true,
        version = "1.0",
        subcommands = {
                ServerCommand.class,
                ClientCommand.class,
                GuiCommand.class
        }
)
public class Main implements Runnable {

    /**
     * Affiche l'aide par défaut si aucune sous-commande n'est spécifiée.
     */
    @Override
    public void run() { CommandLine.usage(this, System.out); }

    /**
     * Point d'entrée JVM : analyse les arguments et exécute la commande appropriée via Picocli.
     */
    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}