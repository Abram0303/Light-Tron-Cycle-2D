package ch.heigvd.gui;

import ch.heigvd.utils.Point;
import java.util.List;

public interface TronMessageListener {
    void onState(String phase,
                 int p1x, int p1y, boolean p1Alive,
                 int p2x, int p2y, boolean p2Alive,
                 List<Point> trails);

    void onGameEnd(String reason, String winnerId);

    void onError(int code, String message);
}

