package app.studybloom.repo;

import java.util.List;
import java.util.function.Consumer;

/**
 * ABSTRACTION: the four things the app does with stored records, with no
 * mention of where they are stored. Calls are asynchronous because the browser
 * talks to Supabase over the network, so results arrive through callbacks.
 */
public interface CrudRepository<T> {
    void findAll(String userId, Consumer<List<T>> onOk, Consumer<RepoError> onError);

    void insert(T item, Consumer<T> onOk, Consumer<RepoError> onError);

    void update(T item, Consumer<T> onOk, Consumer<RepoError> onError);

    void delete(String id, Runnable onOk, Consumer<RepoError> onError);
}
