public class Cell {
    private Ship ship;
    private boolean isHit;

    public boolean hasShip() {
        return ship != null;
    }

    public Ship getShip() {
        return ship;
    }

    public void placeShip(Ship ship) {
        this.ship = ship;
    }

    public boolean isHit() {
        return isHit;
    }

    public void markHit() {
        isHit = true;
    }
}