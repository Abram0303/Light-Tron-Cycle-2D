package ch.heigvd.gui;

import ch.heigvd.utils.Point;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.*;

public class GameCanvas extends Canvas {

    private final ClientGameState state;
    private final Image p1Image;
    private final Image p2Image;

    public GameCanvas(ClientGameState state) {
        this.state = state;
        // Chargement des images (assurez-vous qu'elles sont dans resources/img/)
        p1Image = safeLoadImage("/img/blue_cycle_top.png");
        p2Image = safeLoadImage("/img/yellow_cycle_top.png");
    }

    // Permet au Canvas de dire à son parent qu'il veut bien changer de taille
    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

    private Image safeLoadImage(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            return null; // Pas d'image, on dessinera des ronds
        }
    }

    public void draw() {
        double w = getWidth();
        double h = getHeight();

        // Eviter de dessiner si la fenêtre est invisible ou minuscule
        if (w < 10 || h < 10) return;

        GraphicsContext g = getGraphicsContext2D();

        // 1. Fond général (toute la fenêtre)
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, w, h);

        int cols = state.width;
        int rows = state.height;
        if (cols == 0 || rows == 0) return;

        // 2. Calcul pour garder le Ratio d'aspect (Case carrée)
        double cellWidth = w / cols;
        double cellHeight = h / rows;
        double cellSize = Math.min(cellWidth, cellHeight); // On prend la plus petite dimension pour que ça rentre

        // Calcul des décalages pour centrer le jeu
        double gridWidth = cols * cellSize;
        double gridHeight = rows * cellSize;
        double offsetX = (w - gridWidth) / 2;
        double offsetY = (h - gridHeight) / 2;

        // On déplace l'origine du dessin pour centrer
        g.save();
        g.translate(offsetX, offsetY);

        // --- DESSIN DU JEU ---

        // Fond du plateau de jeu
        var bg = new RadialGradient(0, 0, 0.5, 0.5, 1.0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(20, 25, 40)),
                new Stop(1.0, Color.rgb(10, 10, 20)));
        g.setFill(bg);
        g.fillRect(0, 0, gridWidth, gridHeight);

        // Grille
        g.setStroke(Color.rgb(40, 50, 80));
        g.setLineWidth(0.5);
        for (int x = 0; x <= cols; x += 5) g.strokeLine(x * cellSize, 0, x * cellSize, gridHeight);
        for (int y = 0; y <= rows; y += 5) g.strokeLine(0, y * cellSize, gridWidth, y * cellSize);
        // Bordure autour du terrain
        g.setStroke(Color.WHITE);
        g.setLineWidth(2.0);
        g.strokeRect(0,0, gridWidth, gridHeight);

        // Traces
        synchronized (state.trails) {
            // Optimisation : Pas de BFS ici pour l'instant, couleur simple pour tester l'affichage
            for (Point p : state.trails) {
                g.setFill(Color.LIGHTGRAY);
                // Si vous remettez le BFS, utilisez les couleurs ici
                g.fillRect(p.x() * cellSize, p.y() * cellSize, cellSize, cellSize);
            }
        }

        // Joueurs
        drawPlayer(g, p1Image, state.p1x, state.p1y, cellSize, state.p1Alive, Color.CYAN);
        drawPlayer(g, p2Image, state.p2x, state.p2y, cellSize, state.p2Alive, Color.YELLOW);

        g.restore(); // Annuler le décalage (translate)
    }

    private void drawPlayer(GraphicsContext g, Image img, int x, int y, double size, boolean alive, Color fallbackColor) {
        double px = x * size;
        double py = y * size;

        if (!alive) {
            g.setGlobalAlpha(0.3);
        }

        if (img != null && !img.isError()) {
            // Dessiner l'image un peu plus grande que la case pour l'effet
            g.drawImage(img, px - size * 0.5, py - size * 0.5, size * 2, size * 2);
        } else {
            // Fallback si l'image n'est pas trouvée
            g.setFill(fallbackColor);
            g.fillOval(px, py, size, size);
        }
        g.setGlobalAlpha(1.0);
    }
}