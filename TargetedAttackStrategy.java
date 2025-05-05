import java.util.*;

public class TargetedAttackStrategy implements AttackStrategy {
    private Position firstHit;
    private Position lastHit;
    private Direction currentDirection;
    private boolean directionEstablished = false;
    private boolean tryingOppositeDirection = false;
    private final Random random = new Random();
    private List<Direction> availableDirections;

    public TargetedAttackStrategy() {
        reset();
    }

    // finds the next attack position based on initial attack
    @Override
    public Position getNextAttackPosition(GameBoard board, List<Position> history) {
        if (firstHit == null) {
            return new RandomAttackStrategy().getNextAttackPosition(board, history);
        }

        if (!directionEstablished) {
            //after first hit, try adjacent positions
            if (availableDirections.isEmpty()) {
                // if no directions left to try, revert to random strategy
                return new RandomAttackStrategy().getNextAttackPosition(board, history);
            }

            // pick a random direction from available directions
            int index = random.nextInt(availableDirections.size());
            currentDirection = availableDirections.get(index);
            availableDirections.remove(index);

            Position nextPos = firstHit.getAdjacent(currentDirection);
            if (isValid(nextPos, history)) {
                return nextPos;
            } else {
                // try another direction if this one is invalid
                return getNextAttackPosition(board, history);
            }
        } else if (!tryingOppositeDirection) {
            // continue in current direction
            Position nextPos = lastHit.getAdjacent(currentDirection);
            if (isValid(nextPos, history)) {
                return nextPos;
            } else {
                // if hit board edge or already attacked position, switch to opposite direction from first hit
                tryingOppositeDirection = true;
                currentDirection = currentDirection.getOpposite();
                nextPos = firstHit.getAdjacent(currentDirection);
                if (isValid(nextPos, history)) {
                    return nextPos;
                } else {
                    // if opposite direction isnt valid either, try remaining directions
                    directionEstablished = false;
                    return getNextAttackPosition(board, history);
                }
            }
        } else {
            // continue in opposite direction
            Position nextPos = lastHit.getAdjacent(currentDirection);
            if (isValid(nextPos, history)) {
                return nextPos;
            } else {
                // if cant continue in opposite direction, try remaining directions
                directionEstablished = false;
                tryingOppositeDirection = false;
                return getNextAttackPosition(board, history);
            }
        }
    }

    @Override
    public void recordHit(Position pos, String shipType, boolean isSunk) {
        // first hit
        if (firstHit == null) {
            firstHit = pos;
            lastHit = pos;
        } else {
            // subsequent hit
            lastHit = pos;
            if (!directionEstablished) {
                // now we know which direction the ship extends
                directionEstablished = true;
            }
        }

        if (isSunk) {
            // reset strategy for next ship
            reset();
        }
    }

    @Override
    public void recordMiss(Position pos) {
        if (directionEstablished && !tryingOppositeDirection) {
            // hit edge of ship in current direction, switch to opposite
            tryingOppositeDirection = true;
            currentDirection = currentDirection.getOpposite();
            // reset last hit to first hit
            lastHit = firstHit;
        } else if (directionEstablished && tryingOppositeDirection) {
            // we've now tried both directions along this axis
            // try other directions from first hit
            directionEstablished = false;
            tryingOppositeDirection = false;
        } else {
            // just tried a direction that didn't work
            // getNextAttackPosition will pick another direction
        }
    }

    @Override
    public void reset() {
        firstHit = null;
        lastHit = null;
        currentDirection = null;
        directionEstablished = false;
        tryingOppositeDirection = false;
        availableDirections = new ArrayList<>(Arrays.asList(Direction.values()));
    }

    private boolean isValid(Position pos, List<Position> history) {
        return pos.getX() >= 0 && pos.getX() < 10 &&
                pos.getY() >= 0 && pos.getY() < 10 &&
                !history.contains(pos);
    }
}