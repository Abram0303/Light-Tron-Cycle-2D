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

    /**
     * Point d'entrée JavaFX : configure l'interface, les événements clavier et initialise la connexion réseau.
     */
    @Override
    public void start(Stage stage) {
        state = new ClientGameState(80, 40);
        gameCanvas = new GameCanvas(state);

        StackPane canvasContainer = new StackPane(gameCanvas);
        canvasContainer.setStyle("-fx-background-color: black;");

        // Binding pour le redimensionnement
        gameCanvas.widthProperty().bind(canvasContainer.widthProperty());
        gameCanvas.heightProperty().bind(canvasContainer.heightProperty());

        gameCanvas.widthProperty().addListener(obs -> gameCanvas.draw());
        gameCanvas.heightProperty().addListener(obs -> gameCanvas.draw());

        startButton = new Button("START");
        startButton.setStyle("-fx-font-size: 14px; -fx-background-color: #00ff00; -fx-text-fill: black; -fx-font-weight: bold;");
        startButton.setFocusTraversable(false);

        startButton.setOnAction(e -> {
            if (client != null) {
                client.sendReady();
                startButton.setDisable(true);
                statusLabel.setText("READY envoyé. En attente de l'adversaire...");
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

        root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(canvasContainer);

        Scene scene = new Scene(root, 1000, 600);
        stage.setTitle("Light-Tron-Cycle-2D");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.show();

        // Gestion des entrées clavier (ZQSD, WASD, Flèches)
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

        // Initialisation du client réseau avec gestion des callbacks
        client = new TronNetworkClient("localhost", 2222, "PlayerFX", new TronMessageListener() {

            /**
             * Met à jour l'état local du jeu (positions, traces) à réception d'un paquet STATE.
             */
            @Override
            public void onState(String phase, int p1x, int p1y, boolean p1Alive, int p2x, int p2y, boolean p2Alive, List<Point> t1, List<Point> t2) {
                Platform.runLater(() -> {
                    state.phase = phase;
                    state.p1x = p1x; state.p1y = p1y; state.p1Alive = p1Alive;
                    state.p2x = p2x; state.p2y = p2y; state.p2Alive = p2Alive;

                    // Mise à jour complète des listes locales
                    state.p1Trails.clear();
                    if (!t1.isEmpty()) {
                        state.p1Trails.addAll(t1);
                    }

                    state.p2Trails.clear();
                    if (!t2.isEmpty()) {
                        state.p2Trails.addAll(t2);
                    }

                    statusLabel.setText("Phase: " + phase);
                    gameCanvas.draw();
                });
            }

            /**
             * Gère la fin de partie (affichage du vainqueur et réactivation du bouton Start).
             */
            @Override
            public void onGameEnd(String reason, String winnerId) {
                Platform.runLater(() -> {
                    gameCanvas.draw();
                    statusLabel.setText("FIN: " + reason + " - Gagnant: " + winnerId);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Fin de partie");
                    alert.setHeaderText("GAME OVER");
                    alert.setContentText("Raison : " + reason + "\nGagnant : " + winnerId);
                    alert.show();

                    startButton.setDisable(false);
                });
            }

            /**
             * Affiche les erreurs critiques ou informatives provenant du serveur.
             */
            @Override
            public void onError(int code, String message) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur: " + message);
                    if (code == 0) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur Réseau");
                        alert.setContentText(message);
                        alert.show();
                    }
                });
            }
        });

        client.start();
        Platform.runLater(() -> {
            gameCanvas.draw();
            root.requestFocus();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}