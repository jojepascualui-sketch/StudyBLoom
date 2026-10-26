package app.studybloom.fakes;

import app.studybloom.repo.KeyValueStore;
import java.util.HashMap;
import java.util.Map;

public class MapStore implements KeyValueStore {
    private final Map<String, String> values = new HashMap<>();

    @Override
    public String get(String key) {
        return values.get(key);
    }

    @Override
    public void set(String key, String value) {
        values.put(key, value);
    }

    @Override
    public void remove(String key) {
        values.remove(key);
    }
}
