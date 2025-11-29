package ch.heigvd.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import ch.heigvd.utils.Point;

public class TronGame implements Runnable {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 20;

    private enum Phase {
        LOBBY, RUNNING, GAME_OVER
    }

    private final String matchId;
    private final TronServerClientHandler p1;
    private final TronServerClientHandler p2;
    private final int tickMillis;

    private Phase phase = Phase.LOBBY;
    private int tick = 0; // Compteur de ticks

    // Etat des joueurs
    private int p1x, p1y;
    private int p2x, p2y;
    private Direction p1dir;
    private Direction p2dir;
    private boolean p1Alive = true;
    private boolean p2Alive = true;

    // Case de la grille occupée
    private final boolean[][] trails = new boolean[WIDTH][HEIGHT];

    // Liste des points occupés
    private final List<Point> trailList = new ArrayList<>();

    public TronGame(String matchId,
                    TronServerClientHandler p1,
                    TronServerClientHandler p2,
                    int tickMillis) {
        this.matchId = matchId;
        this.p1 = p1;
        this.p2 = p2;
        this.tickMillis = tickMillis;

        // Positions et directions initiales
        this.p1x = 5;
        this.p1y = 10;
        this.p2x = 35;
        this.p2y = 10;
        this.p1dir = Direction.RIGHT;
        this.p2dir = Direction.LEFT;

        // Marquer les positions de départ comme occupées
        markTrail(p1x, p1y);
        markTrail(p2x, p2y);
    }

    // Lancement de la boucle de jeu indépendamment du reste (thread séparé)
    @Override
    public void run() {
        try {
            phase = Phase.RUNNING;
            sendGameStart();

            while (phase == Phase.RUNNING) {

                // Attendre la durée d'un tick (avancer dans le temps)
                Thread.sleep(tickMillis);
                tick++;

                applyInputs();
                stepSimulation();
                sendState();

                if (!p1Alive || !p2Alive) {
                    endGameAfterCollision();
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Partie interrompue: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Erreur d'E/S dans la partie " + matchId + " : " + e.getMessage());
            // On considère qu'un problème d'E/S termine la partie pour cause de déconnexion
            try {
                sendGameEnd("DISCONNECT", "-");
            } catch (IOException ignored) {
            }
        }
    }

    // ----- Méthodes de gestion de la simulation -----

    private void applyInputs() {

        // Lire la dernière direction demandée
        String d1 = p1.consumePendingDirection();
        String d2 = p2.consumePendingDirection();

        if (d1 != null) {
            p1dir = Direction.fromString(d1);
        }

        if (d2 != null) {
            p2dir = Direction.fromString(d2);
        }
    }

    private void stepSimulation() {
        if (!p1Alive && !p2Alive) {
            return;
        }

        // Calcul des nouvelles positions
        int newP1x = p1x + dx(p1dir);
        int newP1y = p1y + dy(p1dir);
        int newP2x = p2x + dx(p2dir);
        int newP2y = p2y + dy(p2dir);

        // Détection des collisions
        boolean p1Collision = !isInside(newP1x, newP1y) || isTrail(newP1x, newP1y);
        boolean p2Collision = !isInside(newP2x, newP2y) || isTrail(newP2x, newP2y);

        // Collision si les deux arrivent sur la même case (tête-contre-tête)
        if (newP1x == newP2x && newP1y == newP2y) {
            p1Collision = true;
            p2Collision = true;
        }

        // Mise à jour des positions et des traces
        if (!p1Collision) {
            p1x = newP1x;
            p1y = newP1y;
            markTrail(p1x, p1y);
        } else {
            p1Alive = false;
        }

        if (!p2Collision) {
            p2x = newP2x;
            p2y = newP2y;
            markTrail(p2x, p2y);
        } else {
            p2Alive = false;
        }
    }

    private void endGameAfterCollision() throws IOException {
        phase = Phase.GAME_OVER;

        String reason;
        String winnerId;

        if (!p1Alive && !p2Alive) {
            reason = "DOUBLE_KO";
            winnerId = "-";
        } else if (!p1Alive) {
            reason = "COLLISION";
            winnerId = p2.getPlayerId();
        } else {
            reason = "COLLISION";
            winnerId = p1.getPlayerId();
        }

        sendGameEnd(reason, winnerId);
    }

    // ----- Méthodes de communication avec les clients -----

    private void sendGameStart() throws IOException {
        String msg = String.format(
                "GAME_START %s %s %d %d %s %s %d %d %s",
                matchId,
                p1.getPlayerId(), p1x, p1y, p1dir,
                p2.getPlayerId(), p2x, p2y, p2dir
        );
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    private void sendState() throws IOException {
        String playersField = String.format(
                "%s:%d:%d:%s:%d,%s:%d:%d:%s:%d",
                p1.getPlayerId(), p1x, p1y, p1dir, p1Alive ? 1 : 0,
                p2.getPlayerId(), p2x, p2y, p2dir, p2Alive ? 1 : 0
        );

        String trailsField = trailList.isEmpty()
                ? "-"
                : trailList.stream()
                .map(p -> p.x() + ":" + p.y())
                .collect(Collectors.joining(","));

        String msg = String.format(
                "STATE %s t%d %s %s %s",
                matchId,
                tick,
                phase.name(),
                playersField,
                trailsField
        );

        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    private void sendGameEnd(String reason, String winnerId) throws IOException {
        String msg = String.format("GAME_END %s %s", reason, winnerId);
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    // ----- Méthodes utilitaires de gestion de la grille et des collisions -----

    private int dx(Direction d) {
        switch (d) {
            case LEFT : return -1;
            case RIGHT : return 1;
            default : return 0;
        }
    }

    private int dy(Direction d) {
        switch (d) {
            case UP : return -1;
            case DOWN : return 1;
            default : return 0;
        }
    }

    private boolean isInside(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    private boolean isTrail(int x, int y) {
        return trails[x][y];
    }

    private void markTrail(int x, int y) {
        if (!trails[x][y]) {
            trails[x][y] = true;
            trailList.add(new Point(x, y));
        }
    }
}
