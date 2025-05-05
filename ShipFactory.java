public class ShipFactory {
    public enum ShipType {
        DESTROYER(2),
        SUBMARINE(3),
        BATTLESHIP(4),
        CARRIER(5);

        private final int length;

        ShipType(int length) {
            this.length = length;
        }

        public int getLength() {
            return length;
        }
    }

    private GameObserver observer;

    public ShipFactory(GameObserver observer) {
        this.observer = observer;
    }

    public Ship createShip(ShipType type) {
        Ship ship = switch (type) {
            case DESTROYER -> new Destroyer();
            case SUBMARINE -> new Submarine();
            case BATTLESHIP -> new Battleship();
            case CARRIER -> new Carrier();
        };
        ship.addObserver(observer);
        return ship;
    }
}