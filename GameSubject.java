import java.util.ArrayList;
import java.util.List;

// Observable subject
public class GameSubject {
    private List<GameObserver> observers = new ArrayList<>();

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers(String message) {
        for (GameObserver observer : observers) {
            observer.update(message);
        }
    }
}
