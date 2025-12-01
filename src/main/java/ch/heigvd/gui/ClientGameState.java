package ch.heigvd.gui;

import java.util.ArrayList;
import java.util.List;
import ch.heigvd.utils.Point;

public class ClientGameState {

    public final int width;
    public final int height;

    // Positions par défaut pour éviter le (0,0) au démarrage
    public int p1x = 5;
    public int p1y = 20;

    public int p2x = 75;
    public int p2y = 20;

    public boolean p1Alive = true;
    public boolean p2Alive = true;

    public final List<Point> trails = new ArrayList<>();
    public String phase = "LOBBY";

    public ClientGameState(int width, int height) {
        this.width = width;
        this.height = height;
    }
}