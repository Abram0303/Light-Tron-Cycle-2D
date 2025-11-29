package ch.heigvd.gui;

import ch.heigvd.utils.Point;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;


public class TronClientGUI {

    private final ClientGameState state;
    private final GamePanel gamePanel;

    private TronClientConnection connection;


    public TronClientGUI() {
        // Même taille que le serveur
        this.state = new ClientGameState(40, 20);

        // Positions de départ (doivent être alignées au serveur pour plus tard)
        state.p1x = 5;
        state.p1y = 10;
        state.p2x = 35;
        state.p2y = 10;
        state.phase = "RUNNING";

        this.gamePanel = new GamePanel(state);

        JFrame frame = new JFrame("Tron Client GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(gamePanel, BorderLayout.CENTER);
        frame.setSize(800, 500);

        this.connection = new TronClientConnection(
                "localhost",
                2222,
                "Romain",
                new TronMessageListener() {
                    @Override
                    public void onState(String phase,
                                        int p1x, int p1y, boolean p1Alive,
                                        int p2x, int p2y, boolean p2Alive,
                                        java.util.List<Point> trails) {

                        state.phase = phase;
                        state.p1x = p1x;
                        state.p1y = p1y;
                        state.p1Alive = p1Alive;
                        state.p2x = p2x;
                        state.p2y = p2y;
                        state.p2Alive = p2Alive;

                        state.trails.clear();
                        state.trails.addAll(trails);

                        SwingUtilities.invokeLater(gamePanel::repaint);
                    }

                    @Override
                    public void onGameEnd(String reason, String winnerId) {
                        state.phase = "GAME_OVER";
                        SwingUtilities.invokeLater(() -> {
                            gamePanel.repaint();
                            JOptionPane.showMessageDialog(null,
                                    "Fin de partie : " + reason + " (gagnant : " + winnerId + ")");
                        });
                    }

                    @Override
                    public void onError(int code, String message) {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(null,
                                        "Erreur (" + code + ") : " + message,
                                        "Erreur", JOptionPane.ERROR_MESSAGE));
                    }
                }
        );
        this.connection.start();


        // Gestion des touches fléchées pour faire bouger P1 localement
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                String dir = null;

                switch (key) {
                    case KeyEvent.VK_UP:
                        dir = "UP";
                        break;
                    case KeyEvent.VK_DOWN:
                        dir = "DOWN";
                        break;
                    case KeyEvent.VK_LEFT:
                        dir = "LEFT";
                        break;
                    case KeyEvent.VK_RIGHT:
                        dir = "RIGHT";
                        break;
                    default:
                        return;
                }

                if (connection != null) {
                    connection.sendInput(dir);
                }
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TronClientGUI::new);
    }
}
