package ch.heigvd.gui;

import ch.heigvd.utils.Point;
import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {

    private final ClientGameState state;

    public GamePanel(ClientGameState state) {
        this.state = state;
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int cols = state.width;
        int rows = state.height;

        if (cols <= 0 || rows <= 0) {
            return;
        }

        int cellWidth  = getWidth()  / cols;
        int cellHeight = getHeight() / rows;
        int cellSize   = Math.min(cellWidth, cellHeight);

        Graphics2D g2 = (Graphics2D) g.create();

        // Dessin de la grille
        g2.setColor(Color.DARK_GRAY);
        for (int x = 0; x <= cols; x++) {
            int px = x * cellSize;
            g2.drawLine(px, 0, px, rows * cellSize);
        }
        for (int y = 0; y <= rows; y++) {
            int py = y * cellSize;
            g2.drawLine(0, py, cols * cellSize, py);
        }

        // Dessin des traces
        g2.setColor(Color.CYAN);
        for (Point p : state.trails) {
            int px = p.x() * cellSize;
            int py = p.y() * cellSize;
            g2.fillRect(px, py, cellSize, cellSize);
        }

        // Joueur 1
        if (state.p1Alive) {
            g2.setColor(Color.GREEN);
        } else {
            g2.setColor(Color.GRAY);
        }
        int p1px = state.p1x * cellSize;
        int p1py = state.p1y * cellSize;
        g2.fillOval(p1px, p1py, cellSize, cellSize);

        // Joueur 2
        if (state.p2Alive) {
            g2.setColor(Color.RED);
        } else {
            g2.setColor(Color.GRAY);
        }
        int p2px = state.p2x * cellSize;
        int p2py = state.p2y * cellSize;
        g2.fillOval(p2px, p2py, cellSize, cellSize);

        g2.dispose();
    }
}

