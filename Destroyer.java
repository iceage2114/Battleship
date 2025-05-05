// Concrete ship classes
public class Destroyer extends Ship {
    public Destroyer() {
        super(2, "Destroyer");
    }

    @Override
    public String getType() {
        return "Destroyer";
    }
}