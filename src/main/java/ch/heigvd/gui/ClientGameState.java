package ch.heigvd.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.heigvd.utils.Point;

public class ClientGameState {

    public final int width;
    public final int height;

    public int p1x = 5, p1y = 20;
    public int p2x = 75, p2y = 20;

    public boolean p1Alive = true;
    public boolean p2Alive = true;

    // Listes synchronisées pour éviter les crashs entre le Réseau et JavaFX
    public final List<Point> p1Trails = Collections.synchronizedList(new ArrayList<>());
    public final List<Point> p2Trails = Collections.synchronizedList(new ArrayList<>());

    public String phase = "LOBBY";

    public ClientGameState(int width, int height) {
        this.width = width;
        this.height = height;
    }
}