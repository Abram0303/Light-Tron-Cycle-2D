package ch.heigvd;

import ch.heigvd.commands.ClientCommand;
import ch.heigvd.commands.ServerCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "light-tron",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Light-Tron-Cycle-2D - jeu réseau inspiré de Tron.",
        subcommands = {
                ServerCommand.class,
                ClientCommand.class
        }
)
public class Main implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
