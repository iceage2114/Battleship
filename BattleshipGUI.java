import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class BattleshipGUI extends JFrame {
    private GameController controller;
    private JPanel playerBoardPanel;
    private JPanel enemyBoardPanel;
    private JTextArea gameLog;
    private Direction currentDirection = Direction.EAST;
    private JLabel placementInstructions;
    private boolean processingTurn = false;

    public BattleshipGUI() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Battleship Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // create boards
        playerBoardPanel = createBoard(true);
        enemyBoardPanel = createBoard(false);

        // game log
        gameLog = new JTextArea(10, 40);
        gameLog.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(gameLog);

        // placement instructions
        placementInstructions = new JLabel("", JLabel.CENTER);
        placementInstructions.setFont(new Font("Arial", Font.BOLD, 14));

        // direction selection panel
        JPanel directionPanel = new JPanel();
        JButton rotateButton = new JButton("Rotate Ship (Current: " + currentDirection + ")");
        rotateButton.addActionListener(e -> {
            currentDirection = Direction.values()[(currentDirection.ordinal() + 1) % Direction.values().length];
            rotateButton.setText("Rotate Ship (Current: " + currentDirection + ")");
        });

        directionPanel.add(rotateButton);

        // layout
        JPanel boardsPanel = new JPanel(new GridLayout(1, 2, 50, 0));
        boardsPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        boardsPanel.add(createBoardPanel(playerBoardPanel, "Your Fleet"));
        boardsPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        boardsPanel.add(createBoardPanel(enemyBoardPanel, "Enemy Waters"));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(placementInstructions, BorderLayout.NORTH);
        topPanel.add(directionPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(boardsPanel, BorderLayout.CENTER);
        add(logScrollPane, BorderLayout.SOUTH);

        makeButtonsSquare(playerBoardPanel);
        makeButtonsSquare(enemyBoardPanel);

        updatePlacementInstructions();
        updateBoardColors();

        pack();
        setLocationRelativeTo(null);
    }

    // private method to change instructions
    private void updatePlacementInstructions() {
        if (controller == null) return;

        if (controller.isPlacementPhase()) {
            ShipFactory.ShipType currentShip = controller.getCurrentShipTypeToPlace();
            placementInstructions.setText("Place your " + currentShip + " (Length: " +
                    currentShip.getLength() + ") - Current direction: " + currentDirection);
        } else {
            placementInstructions.setText("All ships placed! Attack the enemy board!");
        }
    }

    private void handlePlayerBoardClick(Position pos) {
        if (controller == null) return;

        if (controller.isPlacementPhase()) {
            if (controller.tryPlacePlayerShip(pos, currentDirection)) {
                updateBoardColors();
                updatePlacementInstructions();
                if (!controller.isPlacementPhase()) {
                    enableEnemyBoard(true);
                }
            } else {
                log("Invalid placement for " + controller.getCurrentShipTypeToPlace() +
                        " at " + pos + " facing " + currentDirection);
            }
        }
    }

    // handles the players clicks on the cpu board and executes cpu attack
    private void handleAttackClick(Position pos) {
        if (controller == null || controller.isPlacementPhase() || controller.isGameOver() || processingTurn) {
            return;
        }

        // set processing flag to prevent multiple clicks
        processingTurn = true;

        // execute player move
        boolean hit = controller.playerAttack(pos);
        updateEnemyBoard(pos, hit);
        log("You attacked " + pos + " - " + (hit ? "HIT!" : "Miss"));

        // if game is over, dont do CPU turn
        if (controller.isGameOver()) {
            processingTurn = false;
            return;
        }

        // add delay before CPU's turn using SwingWorker
        // I used AI to help me write this section of SwingWorker
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // 1-second delay
                Thread.sleep(1000);
                return null;
            }

            @Override
            protected void done() {
                // CPU's turn after delay
                Position cpuAttack = controller.cpuAttack();
                if (cpuAttack != null) {  // Check if move was made (not game over)
                    updatePlayerBoard(cpuAttack, controller.isHit(cpuAttack));
                    log("Enemy attacked " + cpuAttack);
                }
                // Reset processing flag
                processingTurn = false;
            }
        }.execute();
    }

    // displays game over pop-up
    private void showGameOverDialog(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
        // disable enemy board clicks
        enableEnemyBoard(false);

        // if player lost, reveal enemy ships
        if (title.contains("Defeat") || message.contains("lost")) {
            revealEnemyShips();
        }
    }

    // turns enemy ships dark gray
    private void revealEnemyShips() {
        if (controller == null) return;

        log("Revealing enemy ship positions...");

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                Position pos = new Position(x, y);
                JButton button = (JButton) enemyBoardPanel.getComponent(y * 10 + x);

                // if position has a ship and hasn't been hit yet
                if (controller.getEnemyBoard().isOccupied(pos) && !controller.getEnemyBoard().isHit(pos)) {
                    button.setBackground(Color.DARK_GRAY);
                }
            }
        }
    }

    private void enableEnemyBoard(boolean enable) {
        for (Component comp : enemyBoardPanel.getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(enable);
            }
        }
    }

    private void updateBoardColors() {
        if (controller == null) return;

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                Position pos = new Position(x, y);
                JButton playerButton = (JButton) playerBoardPanel.getComponent(y * 10 + x);
                JButton enemyButton = (JButton) enemyBoardPanel.getComponent(y * 10 + x);

                // player board
                playerButton.setBackground(Color.BLUE);
                if (controller.getPlayerBoard().isOccupied(pos)) {
                    playerButton.setBackground(Color.DARK_GRAY);
                }

                // enemy board
                enemyButton.setBackground(Color.BLUE);
                if (controller.getEnemyBoard().isHit(pos)) {
                    enemyButton.setBackground(controller.getEnemyBoard().checkHit(pos) != null ?
                            Color.YELLOW : Color.GRAY);
                }
            }
        }
    }

    private void makeButtonsSquare(JPanel boardPanel) {
        for (Component comp : boardPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                Dimension size = button.getPreferredSize();
                int max = Math.max(size.width, size.height);
                button.setPreferredSize(new Dimension(max, max));
                button.setMinimumSize(new Dimension(max, max));
                button.setMaximumSize(new Dimension(max, max));
            }
        }
    }

    private JPanel createBoardPanel(JPanel board, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(board, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBoard(boolean isPlayerBoard) {
        JPanel board = new JPanel(new GridLayout(10, 10, 1, 1));
        board.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // remove all existing components first
        board.removeAll();

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                Position pos = new Position(x, y);
                JButton button = new JButton();
                button.setOpaque(true);
                button.setBorderPainted(false);
                button.setBackground(Color.CYAN);

                // identify the button
                button.setActionCommand(pos.toString());

                // add listener only if not already present
                for (ActionListener al : button.getActionListeners()) {
                    button.removeActionListener(al);
                }

                if (isPlayerBoard) {
                    button.addActionListener(e -> handlePlayerBoardClick(pos));
                } else {
                    button.addActionListener(e -> handleAttackClick(pos));
                }

                board.add(button);
            }
        }
        return board;
    }



    private void updateEnemyBoard(Position pos, boolean hit) {
        JButton button = (JButton) enemyBoardPanel.getComponent(pos.getY() * 10 + pos.getX());
        button.setBackground(hit ? Color.RED : Color.GRAY);
        button.setEnabled(false);
    }

    private void updatePlayerBoard(Position pos, boolean hit) {
        JButton button = (JButton) playerBoardPanel.getComponent(pos.getY() * 10 + pos.getX());
        button.setBackground(hit ? Color.RED : Color.GRAY);
    }

    private void log(String message) {
        gameLog.append(message + "\n");
    }

    public void setController(GameController controller) {
        this.controller = controller;
        // passes a reference to showGameOverDialog method
        controller.setGameOverHandler(this::showGameOverDialog);
        updateBoardColors();
        updatePlacementInstructions();
        enableEnemyBoard(!controller.isPlacementPhase() && !controller.isGameOver());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BattleshipGUI gui = new BattleshipGUI();
            GameController controller = new GameController(gui.new GameLogger());
            gui.setController(controller);
            gui.setVisible(true);
        });
    }

    private class GameLogger implements GameObserver {
        @Override
        public void update(String message) {
            log(message);
            System.out.println(message);
            if (message.contains("sunk")) {
                String title;
                if (message.contains("Your")) {
                    title = "Your Ship Sunk!";
                } else {
                    title = "Enemy Ship Sunk!";
                }
                JOptionPane.showMessageDialog(BattleshipGUI.this,
                        message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}