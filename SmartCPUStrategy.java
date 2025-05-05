import java.util.List;

// this class lets the cpu switch between targeted and random attack strategy
public class SmartCPUStrategy implements AttackStrategy {
    private AttackStrategy currentStrategy;
    private final AttackStrategy randomStrategy;
    private final AttackStrategy targetedStrategy;

    public SmartCPUStrategy() {
        this.randomStrategy = new RandomAttackStrategy();
        this.targetedStrategy = new TargetedAttackStrategy();
        this.currentStrategy = randomStrategy;
    }

    @Override
    public Position getNextAttackPosition(GameBoard board, List<Position> history) {
        return currentStrategy.getNextAttackPosition(board, history);
    }

    @Override
    public void recordHit(Position pos, String shipType, boolean isSunk) {
        currentStrategy.recordHit(pos, shipType, isSunk);

        // switch to targeted strategy after first hit
        if (currentStrategy == randomStrategy && !isSunk) {
            currentStrategy = targetedStrategy;
            targetedStrategy.recordHit(pos, shipType, false);
        }

        // return to random strategy after sinking a ship
        if (isSunk) {
            currentStrategy = randomStrategy;
        }
    }

    @Override
    public void recordMiss(Position pos) {
        currentStrategy.recordMiss(pos);
    }

    @Override
    public void reset() {
        currentStrategy = randomStrategy;
        randomStrategy.reset();
        targetedStrategy.reset();
    }
}