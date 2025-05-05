public class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10) {
            throw new IllegalArgumentException("Position out of bounds");
        }
        this.x = x;
        this.y = y;
    }

    // Getters
    public int getX() { return x; }
    public int getY() { return y; }

    // gets all valid adjacent tiles
    public Position getAdjacent(Direction direction) {
        int newX = x + direction.getXOffset();
        int newY = y + direction.getYOffset();

        // check bounds before creating new Position
        if (newX < 0 || newX >= 10 || newY < 0 || newY >= 10) {
            return null;
        }

        return new Position(newX, newY);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}