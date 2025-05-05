import java.util.List;

public interface AttackStrategy {
    Position getNextAttackPosition(GameBoard opponentBoard, List<Position> attackHistory);
    void recordHit(Position pos, String shipType, boolean isSunk);
    void recordMiss(Position pos);
    void reset();
}