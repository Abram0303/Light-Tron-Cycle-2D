package ch.heigvd.utils;

import java.util.List;

public interface TronMessageListener {
    default void onRawMessage(String msg) {}

    // Signature modifiée : trailsP1 et trailsP2 séparés
    void onState(String phase,
                 int p1x, int p1y, boolean p1Alive,
                 int p2x, int p2y, boolean p2Alive,
                 List<Point> trailsP1,
                 List<Point> trailsP2);

    void onGameEnd(String reason, String winnerId);
    void onError(int code, String message);
}