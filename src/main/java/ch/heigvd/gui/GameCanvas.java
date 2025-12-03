package ch.heigvd.gui;

import ch.heigvd.utils.Point;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

public class GameCanvas extends Canvas {

    private final ClientGameState state;
    private final Image p1Image;
    private final Image p2Image;

    // Couleurs Tron classiques
    private static final Color P1_COLOR = Color.CYAN;
    private static final Color P2_COLOR = Color.GOLD;
    private static final Color GRID_COLOR = Color.rgb(0, 40, 60);

    public GameCanvas(ClientGameState state) {
        this.state = state;
        p1Image = safeLoadImage("/img/blue_cycle_top.png");
        p2Image = safeLoadImage("/img/yellow_cycle_top.png");
    }

    @Override
    public boolean isResizable() { return true; }

    @Override
    public double prefWidth(double height) { return getWidth(); }

    @Override
    public double prefHeight(double width) { return getHeight(); }

    /**
     * Charge une image depuis les ressources de manière sécurisée (ne plante pas si fichier manquant).
     */
    private Image safeLoadImage(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Méthode principale de rendu : dessine la grille, les traînées et les joueurs.
     * Applique également les effets visuels (néon/glow).
     */
    public void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w < 10 || h < 10) return;

        GraphicsContext g = getGraphicsContext2D();

        // Fond noir profond
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, w, h);

        int cols = state.width;
        int rows = state.height;
        if (cols == 0 || rows == 0) return;

        double cellWidth = w / cols;
        double cellHeight = h / rows;
        double cellSize = Math.min(cellWidth, cellHeight);

        double gridWidth = cols * cellSize;
        double gridHeight = rows * cellSize;

        // Cast en int pour éviter le flou sur la grille
        int offsetX = (int) ((w - gridWidth) / 2);
        int offsetY = (int) ((h - gridHeight) / 2);

        g.save();
        g.translate(offsetX, offsetY);

        // Dessin du jeu

        // Fond du plateau
        var bg = new RadialGradient(0, 0, 0.5, 0.5, 1.0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(20, 25, 45)),
                new Stop(1.0, Color.rgb(5, 5, 10)));
        g.setFill(bg);
        g.fillRect(0, 0, gridWidth, gridHeight);

        // Grille style "Synthwave"
        g.setStroke(GRID_COLOR);
        g.setLineWidth(0.5);
        // Lignes verticales
        for (int x = 0; x <= cols; x++) {
            g.strokeLine(x * cellSize, 0, x * cellSize, gridHeight);
        }
        // Lignes horizontales
        for (int y = 0; y <= rows; y++) {
            g.strokeLine(0, y * cellSize, gridWidth, y * cellSize);
        }

        // Bordure brillante
        g.setStroke(Color.WHITE);
        g.setLineWidth(2.0);
        g.strokeRect(0, 0, gridWidth, gridHeight);

        // Traces P1 (Cyan)
        g.setFill(P1_COLOR.deriveColor(0, 1, 1, 0.9));
        synchronized (state.p1Trails) {
            for (Point p : state.p1Trails) {
                g.fillRect(p.x() * cellSize, p.y() * cellSize, cellSize, cellSize);
            }
        }

        // Traces P2 (Jaune/Or)
        g.setFill(P2_COLOR.deriveColor(0, 1, 1, 0.9));
        synchronized (state.p2Trails) {
            for (Point p : state.p2Trails) {
                g.fillRect(p.x() * cellSize, p.y() * cellSize, cellSize, cellSize);
            }
        }

        // Effet de lueur global pour les éléments du jeu
        g.setEffect(new DropShadow(10, Color.rgb(0, 255, 255, 0.3)));

        // Joueurs
        drawPlayer(g, p1Image, state.p1x, state.p1y, cellSize, state.p1Alive, P1_COLOR);
        drawPlayer(g, p2Image, state.p2x, state.p2y, cellSize, state.p2Alive, P2_COLOR);

        g.setEffect(null);
        g.restore();
    }

    /**
     * Dessine un joueur spécifique (sprite ou forme fallback) avec gestion de la transparence si mort.
     */
    private void drawPlayer(GraphicsContext g, Image img, int x, int y, double size, boolean alive, Color fallbackColor) {
        double px = x * size;
        double py = y * size;

        // Fantôme si mort
        if (!alive) {
            g.setGlobalAlpha(0.3);
        }

        if (img != null && !img.isError()) {
            // Dessiner l'image un peu plus grande centrée
            g.drawImage(img, px - size * 0.5, py - size * 0.5, size * 2, size * 2);
        } else {
            // Cas si l'image ne charge pas
            // Fallback : Rond néon
            g.setFill(fallbackColor);
            g.fillOval(px, py, size, size);
            // Petit point blanc au centre pour faire la tête
            g.setFill(Color.WHITE);
            g.fillOval(px + size*0.3, py + size*0.3, size*0.4, size*0.4);
        }
        g.setGlobalAlpha(1.0);
    }
}