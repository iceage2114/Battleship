import java.util.ArrayList;
import java.util.List;

/**
 * GameBoard class
 * Main functions: keep track of each players board
 */
public class GameBoard {
    private final int SIZE = 10;
    public Cell[][] cells;
    private List<Ship> ships;

    public GameBoard() {
        cells = new Cell[SIZE][SIZE];

        // initialize cells
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                cells[i][j] = new Cell();
            }
        }
        ships = new ArrayList<>();
    }

    // places a ship in a direction, starting at a position
    public void placeShip(Ship ship, Position start, Direction direction) {
        // in bounds check
        if (!isShipWithinBounds(start, direction, ship.getLength())) {
            throw new IllegalArgumentException("Ship would extend beyond board boundaries");
        }

        Position current = start;
        for (int i = 0; i < ship.getLength(); i++) {
            if (isOccupied(current)) {
                throw new IllegalArgumentException("Position already occupied");
            }
            cells[current.getX()][current.getY()].placeShip(ship);
            current = current.getAdjacent(direction);
        }
        ships.add(ship);
        ship.markAsPlaced();
    }

    // position on board is hit
    public boolean receiveAttack(Position pos) {
        if (!isValidPosition(pos)) {
            throw new IllegalArgumentException("Attack position out of bounds");
        }
        Cell cell = cells[pos.getX()][pos.getY()];

        // already hit this position
        if (cell.isHit()) {
            return false;
        }

        // cell is hit and ship is marked as hit
        cell.markHit();
        if (cell.hasShip()) {
            Ship ship = cell.getShip();
            ship.hit();
            return true;
        }
        return false;
    }

    public boolean isAllShipsSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }

    // check if a ship can be placed
    public boolean canPlaceShip(Position start, Direction direction, int length) {
        Position current = start;

        // first check if the entire ship would fit in this direction
        if (!isShipWithinBounds(start, direction, length)) {
            return false;
        }

        // check each position along the ship's length
        for (int i = 0; i < length; i++) {
            if (!isValidPosition(current) || isOccupied(current)) {
                return false;
            }
            current = current.getAdjacent(direction);
        }
        return true;
    }

    private boolean isShipWithinBounds(Position start, Direction direction, int length) {
        int x = start.getX();
        int y = start.getY();

        switch (direction) {
            case NORTH:
                return y - (length - 1) >= 0;
            case SOUTH:
                return y + (length - 1) < SIZE;
            case EAST:
                return x + (length - 1) < SIZE;
            case WEST:
                return x - (length - 1) >= 0;
            default:
                return false;
        }
    }

    private boolean isValidPosition(Position pos) {
        return pos.getX() >= 0 && pos.getX() < SIZE &&
                pos.getY() >= 0 && pos.getY() < SIZE;
    }

    public boolean isOccupied(Position pos) {
        return cells[pos.getX()][pos.getY()].hasShip();
    }

    // check if the position has been hit before
    public boolean isHit(Position pos) {
        if (!isValidPosition(pos)) {
            return false;
        }
        Cell cell = cells[pos.getX()][pos.getY()];
        return cell.isHit();
    }

    // returns the ship at the position
    public Ship getShipAt(Position attackPosition) {
        Cell cell = cells[attackPosition.getX()][attackPosition.getY()];
        if (cell.hasShip()) {
            return cell.getShip();
        }
        return null;
    }

    // returns the ship if the attacked position hit a ship
    public Ship checkHit(Position attackPosition) {
        Cell cell = cells[attackPosition.getX()][attackPosition.getY()];
        if (cell.isHit() && cell.hasShip()) {
            return cell.getShip();
        }
        return null;
    }
}