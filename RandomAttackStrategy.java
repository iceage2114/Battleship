import java.util.List;
import java.util.Random;

public class RandomAttackStrategy implements AttackStrategy {
    private final Random random = new Random();

    // finds next random attack position
    @Override
    public Position getNextAttackPosition(GameBoard board, List<Position> history) {
        Position pos;
        int attempts = 0;

        do {
            pos = new Position(random.nextInt(10), random.nextInt(10));
            attempts++;
            if (attempts > 100) { // Fallback if random fails
                for (int x = 0; x < 10; x++) {
                    for (int y = 0; y < 10; y++) {
                        pos = new Position(x, y);
                        if (!history.contains(pos)) return pos;
                    }
                }
            }
        } while (history.contains(pos));

        return pos;
    }

    @Override public void recordHit(Position pos, String shipType, boolean isSunk) {}
    @Override public void recordMiss(Position pos) {}
    @Override public void reset() {}
}