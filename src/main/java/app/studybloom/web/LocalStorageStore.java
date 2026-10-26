package app.studybloom.web;

import app.studybloom.repo.KeyValueStore;
import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.browser.Storage;

/** KeyValueStore backed by window.localStorage. Falls back to memory if storage is blocked. */
public class LocalStorageStore implements KeyValueStore {
    private final Storage storage = Storage.getLocalStorage();
    private final Map<String, String> memory = new HashMap<>();

    @Override
    public String get(String key) {
        try {
            return storage.getItem(key);
        } catch (RuntimeException e) {
            return memory.get(key);
        }
    }

    @Override
    public void set(String key, String value) {
        try {
            storage.setItem(key, value);
        } catch (RuntimeException e) {
            memory.put(key, value);
        }
    }

    @Override
    public void remove(String key) {
        try {
            storage.removeItem(key);
        } catch (RuntimeException e) {
            memory.remove(key);
        }
    }
}
