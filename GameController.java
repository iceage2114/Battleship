import java.util.*;

public class GameController extends GameSubject {
    private final GameBoard playerBoard;
    private final GameBoard enemyBoard;
    private final CPUPlayer cpuPlayer;
    private final ShipFactory shipFactory;
    private final Random random = new Random();
    private ShipFactory.ShipType currentShipTypeToPlace;
    private boolean isPlacementPhase = true;
    private List<ShipFactory.ShipType> remainingShipTypes;
    private boolean gameOver = false;

    @FunctionalInterface
    public interface GameOverHandler {
        void handleGameOver(String title, String message);
    }

    private GameOverHandler gameOverHandler;

    public GameController(GameObserver gameObserver) {
        this.playerBoard = new GameBoard();
        this.enemyBoard = new GameBoard();
        this.cpuPlayer = new CPUPlayer();
        this.shipFactory = new ShipFactory(gameObserver);

        this.remainingShipTypes = new ArrayList<>(Arrays.asList(ShipFactory.ShipType.values()));
        Collections.sort(remainingShipTypes, Comparator.comparingInt(ShipFactory.ShipType::getLength).reversed());
        currentShipTypeToPlace = remainingShipTypes.get(0);

        // places CPU ships and lets user place their own ships
        placeCPUShips();
        notifyObservers("Place your " + currentShipTypeToPlace + " (length: " + currentShipTypeToPlace.getLength() + ")");
    }

    // there is a placement phase at the beginning of the game where the player places their 4 ships
    public boolean isPlacementPhase() {
        return isPlacementPhase;
    }

    // checks if player can place a ship in a position
    public boolean tryPlacePlayerShip(Position pos, Direction dir) {
        if (!isPlacementPhase) return false;

        if (playerBoard.canPlaceShip(pos, dir, currentShipTypeToPlace.getLength())) {
            Ship ship = shipFactory.createShip(currentShipTypeToPlace);
            playerBoard.placeShip(ship, pos, dir);

            // move to next ship
            remainingShipTypes.remove(currentShipTypeToPlace);
            if (!remainingShipTypes.isEmpty()) {
                currentShipTypeToPlace = remainingShipTypes.get(0);
                notifyObservers("Ship placed! Now place your " + currentShipTypeToPlace +
                        " (length: " + currentShipTypeToPlace.getLength() + ")");
            } else {
                isPlacementPhase = false;
                notifyObservers("All ships placed! Game begins - attack the enemy board!");
            }
            return true;
        }
        notifyObservers("Invalid placement for " + currentShipTypeToPlace +
                " at " + pos + " facing " + dir);
        return false;
    }

    public ShipFactory.ShipType getCurrentShipTypeToPlace() {
        return currentShipTypeToPlace;
    }

    // randomly generates positions and directions for the cpu ships
    private void placeCPUShips() {
        for (ShipFactory.ShipType type : ShipFactory.ShipType.values()) {
            boolean placed = false;
            while (!placed) {
                Direction dir = randomDirection();
                Position pos = getValidRandomPosition(type.getLength(), dir);

                if (enemyBoard.canPlaceShip(pos, dir, type.getLength())) {
                    Ship ship = shipFactory.createShip(type);

                    // mark as enemy ship
                    ship.setEnemy(true);
                    enemyBoard.placeShip(ship, pos, dir);
                    placed = true;
                }
            }
        }
        notifyObservers("Enemy ships have been placed.");
    }

    // finds random position in bounds to place the ship
    private Position getValidRandomPosition(int shipLength, Direction dir) {
        // calculate maximum starting position to ensure ship fits within bounds
        int maxX = 10;
        int maxY = 10;

        if (dir == Direction.NORTH || dir == Direction.SOUTH) {
            maxY = 10 - shipLength + 1;
        } else if (dir == Direction.EAST || dir == Direction.WEST) {
            maxX = 10 - shipLength + 1;
        }

        // ensure no negative values
        maxX = Math.max(1, maxX);
        maxY = Math.max(1, maxY);

        // return a position within the safe bounds
        return new Position(
                random.nextInt(maxX),
                random.nextInt(maxY)
        );
    }

    private Direction randomDirection() {
        return Direction.values()[random.nextInt(Direction.values().length)];
    }

    public boolean playerAttack(Position pos) {
        // dont allow attacks if game is over
        if (gameOver) {
            return false;
        }

        // check if this position has already been hit
        if (enemyBoard.isHit(pos)) {
            notifyObservers("This position has already been attacked");
            return false;
        }

        // executes a hit on enemy board
        if (enemyBoard.receiveAttack(pos)) {
            Ship hitShip = enemyBoard.getShipAt(pos);
            notifyObservers("Player HIT at " + pos + "!");
            if (enemyBoard.isAllShipsSunk()) {
                gameOver = true;
                notifyObservers("Player wins! All enemy ships sunk!");
                showGameOverDialog("Victory!", "You have defeated the enemy fleet!");
            }
            return true;
        }
        notifyObservers("Player MISSED at " + pos);
        return false;
    }

    public Position cpuAttack() {
        // dont attack if game is over
        if (gameOver) {
            return null;
        }

        // finds position to attack
        Position attackPos = cpuPlayer.determineAttackPosition(playerBoard);
        boolean hit = playerBoard.receiveAttack(attackPos);

        Ship hitShip = null;
        boolean isSunk = false;
        String shipType = null;

        if (hit) {
            hitShip = playerBoard.getShipAt(attackPos);
            if (hitShip != null) {
                isSunk = hitShip.isSunk();
                shipType = hitShip.getType();
            }

            notifyObservers("Enemy HIT at " + attackPos + "!");
            if (playerBoard.isAllShipsSunk()) {
                gameOver = true;
                notifyObservers("Enemy wins! All your ships sunk!");
                showGameOverDialog("Defeat!", "Your fleet has been destroyed!");
            }
        } else {
            notifyObservers("Enemy MISSED at " + attackPos);
        }

        // update cpus knowledge with the result
        cpuPlayer.recordAttackResult(attackPos, hit, shipType, isSunk);

        return attackPos;
    }

    public boolean isHit(Position pos) {
        return playerBoard.isHit(pos) && playerBoard.isOccupied(pos);
    }

    public GameBoard getPlayerBoard() {
        return playerBoard;
    }

    public GameBoard getEnemyBoard() {
        return enemyBoard;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOverHandler(GameOverHandler handler) {
        this.gameOverHandler = handler;
    }

    // update the showGameOverDialog method to use the handler
    private void showGameOverDialog(String title, String message) {
        if (gameOverHandler != null) {
            gameOverHandler.handleGameOver(title, message);
        }
    }
}