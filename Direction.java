import java.util.List;

// this enum to support cardinal directions
public enum Direction {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int xOffset;
    private final int yOffset;

    Direction(int xOffset, int yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    // Getters for offsets
    public int getXOffset() { return xOffset; }
    public int getYOffset() { return yOffset; }

    // Get a random direction
    public static Direction getRandomDirection() {
        Direction[] directions = values();
        return directions[(int) (Math.random() * directions.length)];
    }

    // Get opposite direction
    public Direction getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }

    // Get all directions as a list
    public static List<Direction> getAllDirections() {
        return List.of(values());
    }
}