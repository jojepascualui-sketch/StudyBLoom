package app.studybloom.repo;

/** Small persistent string store: localStorage in the browser, a HashMap in tests. */
public interface KeyValueStore {
    String get(String key);

    void set(String key, String value);

    void remove(String key);

    default boolean getBoolean(String key, boolean fallback) {
        String value = get(key);
        return value == null ? fallback : "true".equals(value);
    }

    default void setBoolean(String key, boolean value) {
        set(key, value ? "true" : "false");
    }
}
