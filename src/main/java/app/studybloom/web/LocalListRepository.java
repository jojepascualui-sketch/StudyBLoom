package app.studybloom.web;

import app.studybloom.repo.CrudRepository;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.RepoError;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * INHERITANCE: shared code for "local demo mode", used when no Supabase
 * project is configured. Records live in localStorage on this device only.
 */
public abstract class LocalListRepository<T> implements CrudRepository<T> {
    private final KeyValueStore store;
    private final String key;
    private final JsonCodec<T> codec;

    protected LocalListRepository(KeyValueStore store, String key, JsonCodec<T> codec) {
        this.store = store;
        this.key = key;
        this.codec = codec;
    }

    @Override
    public void findAll(String userId, Consumer<List<T>> onOk, Consumer<RepoError> onError) {
        onOk.accept(readAll());
    }

    @Override
    public void insert(T item, Consumer<T> onOk, Consumer<RepoError> onError) {
        List<T> items = readAll();
        items.add(item);
        writeAll(items);
        onOk.accept(item);
    }

    @Override
    public void update(T item, Consumer<T> onOk, Consumer<RepoError> onError) {
        List<T> items = readAll();
        for (int i = 0; i < items.size(); i++) {
            if (codec.idOf(items.get(i)).equals(codec.idOf(item))) {
                items.set(i, item);
                writeAll(items);
                onOk.accept(item);
                return;
            }
        }
        onError.accept(RepoError.of("That item no longer exists."));
    }

    @Override
    public void delete(String id, Runnable onOk, Consumer<RepoError> onError) {
        List<T> items = readAll();
        items.removeIf(item -> codec.idOf(item).equals(id));
        writeAll(items);
        onOk.run();
    }

    private List<T> readAll() {
        List<T> items = new ArrayList<>();
        String saved = store.get(key);
        if (saved != null) {
            for (Json row : Json.parse(saved).items()) {
                items.add(codec.fromJson(row));
            }
        }
        return items;
    }

    private void writeAll(List<T> items) {
        Json array = Json.array();
        for (T item : items) {
            array.add(codec.toJson(item));
        }
        store.set(key, array.stringify());
    }
}
