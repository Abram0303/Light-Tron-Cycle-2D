package ch.heigvd.commands;

import ch.heigvd.gui.TronClientFX;
import picocli.CommandLine.Command;

@Command(name = "gui", description = "Lance l'interface graphique.")
public class GuiCommand implements Runnable {
    @Override
    public void run() {
        TronClientFX.main(new String[0]);
    }
}