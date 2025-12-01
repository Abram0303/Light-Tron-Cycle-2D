package ch.heigvd.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TronGame implements Runnable {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 40;
    private enum Phase { LOBBY, RUNNING, GAME_OVER }

    private final String matchId;
    private final TronServerClientHandler p1;
    private final TronServerClientHandler p2;
    private final int tickMillis;

    private Phase phase = Phase.LOBBY;
    private int tick = 0;
    private int p1x = 10, p1y = 20, p2x = 70, p2y = 20;
    private Direction p1dir = Direction.RIGHT, p2dir = Direction.LEFT;
    private boolean p1Alive = true, p2Alive = true;

    // Matrice pour collision rapide O(1)
    private final boolean[][] trailsMap = new boolean[WIDTH][HEIGHT];

    // Listes séparées pour l'envoi au client
    private final List<Point> p1Trails = new ArrayList<>();
    private final List<Point> p2Trails = new ArrayList<>();

    public TronGame(String matchId, TronServerClientHandler p1, TronServerClientHandler p2, int tickMillis) {
        this.matchId = matchId;
        this.p1 = p1;
        this.p2 = p2;
        this.tickMillis = tickMillis;
        // Marquer les positions initiales
        markTrail(p1x, p1y, 1);
        markTrail(p2x, p2y, 2);
    }

    @Override
    public void run() {
        try {
            phase = Phase.RUNNING;
            sendGameStart();

            long lastTime = System.currentTimeMillis();

            while (phase == Phase.RUNNING) {
                long now = System.currentTimeMillis();
                long elapsed = now - lastTime;

                if (elapsed >= tickMillis) {
                    lastTime = now;
                    tick++;
                    applyInputs();
                    stepSimulation();
                    sendState();
                    if (!p1Alive || !p2Alive) endGameAfterCollision();
                } else {
                    Thread.sleep(Math.max(1, tickMillis - elapsed));
                }
            }
        } catch (InterruptedException | IOException e) {
            System.err.println("Fin de partie (erreur/interruption): " + e.getMessage());
        }
    }

    private void applyInputs() {
        String d1 = p1.consumePendingDirection();
        String d2 = p2.consumePendingDirection();
        if (d1 != null) p1dir = Direction.fromString(d1);
        if (d2 != null) p2dir = Direction.fromString(d2);
    }

    private void stepSimulation() {
        if (!p1Alive && !p2Alive) return;
        int nx1 = p1x + dx(p1dir), ny1 = p1y + dy(p1dir);
        int nx2 = p2x + dx(p2dir), ny2 = p2y + dy(p2dir);

        boolean c1 = !isInside(nx1, ny1) || isTrail(nx1, ny1);
        boolean c2 = !isInside(nx2, ny2) || isTrail(nx2, ny2);

        // Collision frontale
        if (nx1 == nx2 && ny1 == ny2) { c1 = true; c2 = true; }

        if (!c1) { p1x = nx1; p1y = ny1; markTrail(p1x, p1y, 1); } else p1Alive = false;
        if (!c2) { p2x = nx2; p2y = ny2; markTrail(p2x, p2y, 2); } else p2Alive = false;
    }

    private void endGameAfterCollision() throws IOException {
        phase = Phase.GAME_OVER;
        String r = (!p1Alive && !p2Alive) ? "DOUBLE_KO" : "COLLISION";
        String w = (!p1Alive && !p2Alive) ? "-" : (p1Alive ? p1.getPlayerId() : p2.getPlayerId());
        String msg = "GAME_END " + r + " " + w;
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    private void sendGameStart() throws IOException {
        String msg = String.format("GAME_START %s %s %d %d %s %s %d %d %s",
                matchId, p1.getPlayerId(), p1x, p1y, p1dir, p2.getPlayerId(), p2x, p2y, p2dir);
        p1.sendMessage(msg); p2.sendMessage(msg);
    }

    private void sendState() throws IOException {
        String pStr = String.format("%s:%d:%d:%s:%d,%s:%d:%d:%s:%d",
                p1.getPlayerId(), p1x, p1y, p1dir, p1Alive?1:0, p2.getPlayerId(), p2x, p2y, p2dir, p2Alive?1:0);

        // Sérialisation des deux listes
        String t1Str = p1Trails.isEmpty() ? "-" : p1Trails.stream().map(Point::toString).collect(Collectors.joining(","));
        String t2Str = p2Trails.isEmpty() ? "-" : p2Trails.stream().map(Point::toString).collect(Collectors.joining(","));

        // PROTOCOLE MODIFIÉ : Ajout de t2Str à la fin
        String msg = String.format("STATE %s %d %s %s %s %s", matchId, tick, phase.name(), pStr, t1Str, t2Str);
        p1.sendMessage(msg); p2.sendMessage(msg);
    }

    private int dx(Direction d) { return d == Direction.LEFT ? -1 : (d == Direction.RIGHT ? 1 : 0); }
    private int dy(Direction d) { return d == Direction.UP ? -1 : (d == Direction.DOWN ? 1 : 0); }
    private boolean isInside(int x, int y) { return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT; }
    private boolean isTrail(int x, int y) { return trailsMap[x][y]; }

    private void markTrail(int x, int y, int playerNum) {
        if(!trailsMap[x][y]) {
            trailsMap[x][y] = true;
            if (playerNum == 1) p1Trails.add(new Point(x,y));
            else p2Trails.add(new Point(x,y));
        }
    }
}