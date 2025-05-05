import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Ship extends GameSubject {
    protected final int length;
    protected int hits;
    protected String name;
    private boolean sunk = false;
    private boolean isPlaced = false;
    protected boolean isEnemy = false;

    public Ship(int length, String name) {
        this.length = length;
        this.hits = 0;
        this.name = name;
    }

    public void setEnemy(boolean isEnemy) {
        this.isEnemy = isEnemy;
    }

    public void markAsPlaced() {
        this.isPlaced = true;
    }

    public void hit() {
        if (!sunk && isPlaced) {
            hits = Math.min(hits + 1, length);
            if (hits >= length) {
                sunk = true;
                String owner = isEnemy ? "Enemy " : "Your ";
                notifyObservers(owner + name + " has been sunk!");
            }
        }
    }

    public boolean isSunk() {
        return hits >= length;
    }

    public int getLength() {
        return length;
    }

    public abstract String getType();
}