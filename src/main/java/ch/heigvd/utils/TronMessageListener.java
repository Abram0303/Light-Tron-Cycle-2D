package ch.heigvd.utils;

import java.util.List;

public interface TronMessageListener {
    // Appelé pour afficher les logs bruts
    default void onRawMessage(String msg) {}

    // Appelé quand l'état du jeu change
    void onState(String phase,
                 int p1x, int p1y, boolean p1Alive,
                 int p2x, int p2y, boolean p2Alive,
                 List<Point> trails);

    void onGameEnd(String reason, String winnerId);
    void onError(int code, String message);
}