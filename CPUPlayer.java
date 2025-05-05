import java.util.ArrayList;
import java.util.List;

public class CPUPlayer {
    private AttackStrategy strategy;
    private List<Position> attackHistory;

    public CPUPlayer() {
        this.strategy = new SmartCPUStrategy();
        this.attackHistory = new ArrayList<>();
    }

    // finds the next attack position
    public Position determineAttackPosition(GameBoard board) {
        Position nextAttack;

        // makes sure it doesnt attack a position thats already hit
        do {
            nextAttack = strategy.getNextAttackPosition(board, attackHistory);
        } while (board.isHit(nextAttack));

        // record this attack
        attackHistory.add(nextAttack);
        return nextAttack;
    }


    public void recordAttackResult(Position pos, boolean hit, String shipType, boolean isSunk) {
        // record the attack in history
        attackHistory.add(pos);

        // update strategy
        if (hit) {
            strategy.recordHit(pos, shipType, isSunk);
        } else {
            strategy.recordMiss(pos);
        }
    }
}