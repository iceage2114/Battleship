public class Carrier extends Ship {
    public Carrier() {
        super(5, "Aircraft Carrier");
    }

    @Override
    public String getType() {
        return "Aircraft Carrier";
    }
}
