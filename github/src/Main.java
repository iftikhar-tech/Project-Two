// JavaFX-based BFS Visualizer in a Ring Topology Graph
// Author: IFTIKHAR.
// Purpose: Visual demonstration of BFS traversal with node/edge manipulation and circular layout

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.*;

public class Main extends Application {
    // Graph-related structures
    private final Map<String, Circle> nodes = new HashMap<>(); // Node visuals
    private final Map<String, List<String>> graph = new HashMap<>(); // Adjacency list
    private final Map<String, double[]> coordinates = new HashMap<>(); // Node positions
    private final Map<String, String> parent = new HashMap<>(); // BFS parent tracking

    // Scene dimensions
    private final int SCENE_WIDTH = 1000;
    private final int SCENE_HEIGHT = 700;

    // UI and container nodes
    private final Group graphGroup = new Group(); // Graph visual group
    private ComboBox<String> startSelector, endSelector, connectToSelector, removeSelector, edgeFromSelector, edgeToSelector;
    private int nodeCount = 0;
    private TextField nodeInputField;

    /**
     * JavaFX application entry point
     */
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane(); // Main layout container
        root.setStyle("-fx-background-color: #0d0d0d;");

        // UI ComboBoxes with prompt text
        startSelector = combo("Start Node");
        endSelector = combo("End Node");
        connectToSelector = combo("Connect To Node");
        removeSelector = combo("Select Node to Remove");
        edgeFromSelector = combo("From");
        edgeToSelector = combo("To");

        // Node label input field
        nodeInputField = new TextField();
        nodeInputField.setPromptText("Node Label");

        // Buttons for graph operations
        Button addNodeBtn = whiteButton("Add Node", e -> addNodeCircular(nodeInputField.getText().toUpperCase()));
        Button removeNodeBtn = whiteButton("Remove Node", e -> removeNode());
        Button addEdgeBtn = whiteButton("Add Edge", e -> {
            String from = edgeFromSelector.getValue();
            String to = edgeToSelector.getValue();
            if (from != null && to != null && !graph.get(from).contains(to)) {
                connect(from, to);
                drawLineSafe(from, to, Color.DARKGRAY);
                log("Edge " + from + "-" + to + " added");
            }
        });

        // Top control bar (add/remove/connect nodes and edges)
        HBox topControls = new HBox(10, connectToSelector, addNodeBtn, removeSelector, removeNodeBtn, edgeFromSelector, edgeToSelector, addEdgeBtn);
        topControls.setPadding(new Insets(10));
        topControls.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #6e00ff; -fx-border-width: 0 0 2 0;");
        root.setTop(topControls);

        // Center area with graph visuals
        StackPane graphPane = new StackPane(graphGroup);
        VBox centerContainer = new VBox(graphPane);
        VBox.setVgrow(graphPane, Priority.ALWAYS);
        root.setCenter(centerContainer);

        // Bottom control bar (BFS execution)
        Button runBFS = whiteButton("Run BFS", e -> {
            resetColors();
            String start = startSelector.getValue();
            String end = endSelector.getValue();
            if (start != null && end != null) {
                new Thread(() -> bfs(start, end)).start(); // BFS runs in separate thread
            }
        });

        Button resetBtn = whiteButton("Reset", e -> reset());

        HBox bottomControls = new HBox(10, startSelector, endSelector, runBFS, resetBtn, nodeInputField);
        bottomControls.setPadding(new Insets(15, 10, 15, 10));
        bottomControls.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #6e00ff; -fx-border-width: 2 0 0 0;");
        root.setBottom(bottomControls);

        reset(); // Initialize graph

        // Final setup
        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT, Color.web("#0d0d0d"));
        stage.setTitle("🌌 BFS Routing Visualizer (Ring Topology)");
        stage.setScene(scene);
        stage.show();
    }

    /** Helper: Creates a styled combo box */
    private ComboBox<String> combo(String prompt) {
        ComboBox<String> cb = new ComboBox<>();
        cb.setPromptText(prompt);
        cb.setStyle("-fx-background-color: white; -fx-text-fill: black;");
        return cb;
    }

    /** Helper: Creates a styled white button with action */
    private Button whiteButton(String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(label);
        btn.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: red;" +
                        "-fx-border-color: #6e00ff;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 5 10 5 10;"
        );
        btn.setOnAction(handler);
        return btn;
    }

    /**
     * Add a new node to the graph in a circular arrangement
     */
    private void addNodeCircular(String label) {
        String connectTo = connectToSelector.getValue();

        if (!label.isEmpty() && !nodes.containsKey(label)) {
            graph.put(label, new ArrayList<>());
            nodeCount++;

            // Recalculate circular layout
            List<String> keys = new ArrayList<>(graph.keySet());
            Collections.sort(keys);

            double centerX = SCENE_WIDTH / 2.0;
            double centerY = SCENE_HEIGHT / 2.0;
            double radius = 120;

            coordinates.clear();
            for (int i = 0; i < keys.size(); i++) {
                double angle = 2 * Math.PI * i / keys.size();
                double x = centerX + radius * Math.cos(angle);
                double y = centerY + radius * Math.sin(angle);
                coordinates.put(keys.get(i), new double[]{x, y});
            }

            // Optional connections
            if (connectTo != null && graph.containsKey(connectTo)) {
                connect(label, connectTo);
                log("Connected " + label + " to " + connectTo);
            }

            if (graph.containsKey("A") && !graph.get(label).contains("A")) {
                connect(label, "A");
                log("Also connected " + label + " to A to complete the circle");
            }

            // Redraw everything
            graphGroup.getChildren().clear();
            nodes.clear();
            for (String node : keys) {
                double[] pos = coordinates.get(node);
                Circle circle = createNeonNode(pos[0], pos[1], node);
                nodes.put(node, circle);
                graphGroup.getChildren().add(circle);
            }

            // Draw edges
            for (String from : graph.keySet()) {
                for (String to : graph.get(from)) {
                    if (from.compareTo(to) < 0) {
                        drawLineSafe(from, to, Color.DARKGRAY);
                    }
                }
            }

            // Update UI dropdowns
            startSelector.getItems().add(label);
            endSelector.getItems().add(label);
            connectToSelector.getItems().add(label);
            removeSelector.getItems().add(label);
            edgeFromSelector.getItems().add(label);
            edgeToSelector.getItems().add(label);

            log("Node " + label + " added with circular update.");
        }
    }

    /** Removes a node from the graph and UI */
    private void removeNode() {
        String node = removeSelector.getValue();
        if (node != null && graph.containsKey(node)) {
            for (String neighbor : graph.get(node)) {
                graph.get(neighbor).remove(node);
            }
            graph.remove(node);
            coordinates.remove(node);
            Circle circle = nodes.remove(node);
            if (circle != null) Platform.runLater(() -> graphGroup.getChildren().remove(circle));

            // Update dropdowns
            startSelector.getItems().remove(node);
            endSelector.getItems().remove(node);
            connectToSelector.getItems().remove(node);
            removeSelector.getItems().remove(node);
            edgeFromSelector.getItems().remove(node);
            edgeToSelector.getItems().remove(node);

            log("Node " + node + " removed.");
        }
    }

    /** Clears and rebuilds the default circular graph */
    private void reset() {
        graphGroup.getChildren().clear();
        graph.clear();
        coordinates.clear();
        nodes.clear();
        parent.clear();
        nodeCount = 0;

        startSelector.getItems().clear();
        endSelector.getItems().clear();
        connectToSelector.getItems().clear();
        removeSelector.getItems().clear();
        edgeFromSelector.getItems().clear();
        edgeToSelector.getItems().clear();

        buildGraph();
        drawGraph();

        for (String label : graph.keySet()) {
            startSelector.getItems().add(label);
            endSelector.getItems().add(label);
            connectToSelector.getItems().add(label);
            removeSelector.getItems().add(label);
            edgeFromSelector.getItems().add(label);
            edgeToSelector.getItems().add(label);
        }

        log("Graph reset.");
    }

    /** Initializes a ring topology */
    private void buildGraph() {
        String[] labels = {"A", "B", "C", "D", "E", "F", "G"};
        double centerX = SCENE_WIDTH / 2.0;
        double centerY = SCENE_HEIGHT / 2.0;
        double radius = 120;

        for (int i = 0; i < labels.length; i++) {
            double angle = 2 * Math.PI * i / labels.length;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            coordinates.put(labels[i], new double[]{x, y});
            graph.put(labels[i], new ArrayList<>());
        }

        for (int i = 0; i < labels.length; i++) {
            String from = labels[i];
            String to = labels[(i + 1) % labels.length]; // Ring connection
            connect(from, to);
        }
    }

    /** Draws nodes and all edges */
    private void drawGraph() {
        for (String label : coordinates.keySet()) {
            double[] pos = coordinates.get(label);
            Circle circle = createNeonNode(pos[0], pos[1], label);
            nodes.put(label, circle);
            graphGroup.getChildren().add(circle);
        }

        for (String from : graph.keySet()) {
            for (String to : graph.get(from)) {
                if (from.compareTo(to) < 0) {
                    drawLineSafe(from, to, Color.DARKGRAY);
                }
            }
        }
    }

    /** Creates a glowing neon circle representing a node */
    private Circle createNeonNode(double x, double y, String label) {
        Circle circle = new Circle(x, y, 20);
        circle.setFill(Color.web("#ff4d4d"));
        circle.setStroke(Color.web("#00ffe1"));
        circle.setStrokeWidth(2);
        circle.setEffect(new DropShadow(18, Color.web("#00ffe1")));

        Tooltip tip = new Tooltip("Node: " + label + "\nConnections: " + graph.get(label).size());
        Tooltip.install(circle, tip);

        Text text = new Text(x - 5, y + 5, label);
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Consolas", 14));
        Platform.runLater(() -> graphGroup.getChildren().add(text));

        return circle;
    }

    /** Connects two nodes in both directions (undirected edge) */
    private void connect(String from, String to) {
        if (!graph.get(from).contains(to)) graph.get(from).add(to);
        if (!graph.get(to).contains(from)) graph.get(to).add(from);
    }

    /** Draws a line (edge) between two nodes */
    private void drawLineSafe(String from, String to, Color color) {
        double[] fromPos = coordinates.get(from);
        double[] toPos = coordinates.get(to);
        Platform.runLater(() -> {
            Line line = new Line(fromPos[0], fromPos[1], toPos[0], toPos[1]);
            line.setStroke(color);
            line.setStrokeWidth(2);
            graphGroup.getChildren().add(line);
        });
    }

    /** BFS traversal and animation */
    private void bfs(String start, String end) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        parent.clear();

        queue.add(start);
        visited.add(start);
        highlight(start, Color.LIME);
        log("Starting BFS from: " + start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            log("Visiting: " + current);

            if (current.equals(end)) {
                log("Destination " + end + " found.");
                drawPath(start, end);
                return;
            }

            for (String neighbor : graph.get(current)) {
                if (!visited.contains(neighbor)) {
                    parent.put(neighbor, current);
                    visited.add(neighbor);
                    queue.add(neighbor);
                    log("Queueing: " + neighbor + " from: " + current);
                    highlight(neighbor, Color.ORANGE);
                    sleep(500); // Animation delay
                }
            }
        }

        log("Destination " + end + " not reachable.");
    }

    /** Draws the shortest path found by BFS */
    private void drawPath(String start, String end) {
        String current = end;
        while (!current.equals(start)) {
            String prev = parent.get(current);
            drawLineSafe(prev, current, Color.web("#00ffcc")); // Path highlight
            current = prev;
        }
        log("Path drawn in neon teal from " + start + " to " + end);
    }

    /** Highlights a node with specified color */
    private void highlight(String nodeId, Color color) {
        Circle circle = nodes.get(nodeId);
        if (circle != null) {
            Platform.runLater(() -> circle.setFill(color));
        }
    }

    /** Resets all node colors to default */
    private void resetColors() {
        for (Circle c : nodes.values()) {
            Platform.runLater(() -> c.setFill(Color.web("#ff4d4d")));
        }
    }

    /** Helper sleep method for animation timing */
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    /** Logger */
    private void log(String message) {
        System.out.println("[LOG] " + message);
    }

    /** Launch the JavaFX application */
    public static void main(String[] args) {
        launch();
    }
}
