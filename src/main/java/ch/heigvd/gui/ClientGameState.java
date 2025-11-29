package ch.heigvd.gui;

import java.util.ArrayList;
import java.util.List;
import ch.heigvd.utils.Point;

public class ClientGameState {

    public final int width;
    public final int height;

    // Positions des joueurs
    public int p1x, p1y;
    public int p2x, p2y;

    public boolean p1Alive = true;
    public boolean p2Alive = true;

    // Liste des cases de trail
    public final List<Point> trails = new ArrayList<>();

    // Phase textuelle : "LOBBY", "RUNNING", "GAME_OVER"
    public String phase = "LOBBY";

    public ClientGameState(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
