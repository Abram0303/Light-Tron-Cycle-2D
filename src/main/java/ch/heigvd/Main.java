package ch.heigvd;

import ch.heigvd.commands.ClientCommand;
import ch.heigvd.commands.GuiCommand;
import ch.heigvd.commands.ServerCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

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
    @Override
    public void run() { CommandLine.usage(this, System.out); }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}