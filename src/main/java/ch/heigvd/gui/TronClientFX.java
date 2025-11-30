package ch.heigvd.gui;

import ch.heigvd.utils.Point;
import ch.heigvd.utils.TronMessageListener;
import ch.heigvd.utils.TronNetworkClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.List;

public class TronClientFX extends Application {

    private ClientGameState state;
    private GameCanvas gameCanvas;
    private TronNetworkClient client;

    private Label statusLabel;
    private Button startButton;
    private BorderPane root;

    @Override
    public void start(Stage stage) {
        // 1. Initialisation de l'état du jeu (80x40 est la taille standard du serveur)
        state = new ClientGameState(80, 40);

        // 2. Création du Canvas de jeu
        gameCanvas = new GameCanvas(state);

        // --- CORRECTIF REDIMENSIONNEMENT ---
        // On place le Canvas dans un StackPane. Le StackPane va centrer le Canvas.
        // Le Canvas va "binder" sa taille sur celle du StackPane.
        StackPane canvasContainer = new StackPane(gameCanvas);
        canvasContainer.setStyle("-fx-background-color: black;"); // Fond noir pour remplir les bandes vides

        // Le canvas prend toujours la taille disponible dans le conteneur
        gameCanvas.widthProperty().bind(canvasContainer.widthProperty());
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty());

        // Important : forcer le redessin quand la fenêtre change de taille
        gameCanvas.widthProperty().addListener(obs -> gameCanvas.draw());
        gameCanvas.heightProperty().addListener(obs -> gameCanvas.draw());
        // -----------------------------------

        // 3. Barre supérieure (Top Bar)
        startButton = new Button("START");
        startButton.setStyle("-fx-font-size: 14px; -fx-background-color: #00ff00; -fx-text-fill: black; -fx-font-weight: bold;");
        startButton.setFocusTraversable(false); // Empêche le bouton de voler le focus clavier (pour les flèches)

        startButton.setOnAction(e -> {
            if (client != null) {
                client.sendReady();
                startButton.setDisable(true);
                statusLabel.setText("READY envoyé. En attente de l'adversaire...");
                // On remet le focus sur le jeu pour être prêt à tourner
                root.requestFocus();
            }
        });

        statusLabel = new Label("Connecté. Cliquez sur START.");
        statusLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, startButton, statusLabel, spacer);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: #111522; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        // 4. Layout Principal
        root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(canvasContainer); // On met le conteneur (StackPane) au centre

        Scene scene = new Scene(root, 1000, 600);

        stage.setTitle("Light-Tron-Cycle-2D");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.show();

        // 5. Gestion des Entrées Clavier
        scene.setOnKeyPressed(event -> {
            if (client == null) return;

            KeyCode code = event.getCode();
            switch (code) {
                case UP, Z, W -> client.sendInput("UP");
                case DOWN, S -> client.sendInput("DOWN");
                case LEFT, Q, A -> client.sendInput("LEFT");
                case RIGHT, D -> client.sendInput("RIGHT");
            }
        });

        // 6. Démarrage du Client Réseau
        // Note: Idéalement, l'IP et le Port devraient venir des arguments du main,
        // mais ici on hardcode localhost pour simplifier le lancement GUI par défaut.
        client = new TronNetworkClient("localhost", 2222, "PlayerFX", new TronMessageListener() {
            @Override
            public void onState(String phase, int p1x, int p1y, boolean p1Alive, int p2x, int p2y, boolean p2Alive, List<Point> trails) {
                // Mise à jour de l'UI obligatoirement dans le thread JavaFX
                Platform.runLater(() -> {
                    state.phase = phase;
                    state.p1x = p1x; state.p1y = p1y; state.p1Alive = p1Alive;
                    state.p2x = p2x; state.p2y = p2y; state.p2Alive = p2Alive;

                    // Synchronisation pour éviter les problèmes de modification concurrente de liste
                    if (!trails.isEmpty()) {
                        synchronized (state.trails) {
                            state.trails.clear();
                            state.trails.addAll(trails);
                        }
                    } else if (phase.equals("RUNNING") && state.trails.isEmpty()) {
                        // Si c'est le début, on vide les trails
                        synchronized (state.trails) {
                            state.trails.clear();
                        }
                    }

                    statusLabel.setText("Phase: " + phase);
                    gameCanvas.draw();
                });
            }

            @Override
            public void onGameEnd(String reason, String winnerId) {
                Platform.runLater(() -> {
                    gameCanvas.draw(); // Dernier dessin pour voir le crash
                    statusLabel.setText("FIN: " + reason + " - Gagnant: " + winnerId);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Fin de partie");
                    alert.setHeaderText("GAME OVER");
                    alert.setContentText("Raison : " + reason + "\nGagnant : " + winnerId);
                    alert.show();

                    startButton.setDisable(false);
                });
            }

            @Override
            public void onError(int code, String message) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur: " + message);
                    if (code == 0) { // Erreur de connexion critique
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur Réseau");
                        alert.setContentText(message);
                        alert.show();
                    }
                });
            }
        });

        client.start();

        // 7. S'assurer que le jeu a le focus au démarrage pour capter les touches
        Platform.runLater(() -> {
            gameCanvas.draw(); // Premier dessin (init positions)
            root.requestFocus();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}