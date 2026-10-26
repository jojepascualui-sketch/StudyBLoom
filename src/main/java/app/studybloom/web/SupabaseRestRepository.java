package app.studybloom.web;

import app.studybloom.repo.CrudRepository;
import app.studybloom.repo.RepoError;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * INHERITANCE: the shared PostgREST code for one table. Subclasses only say
 * which table, which sort order, and which codec to use.
 * ABSTRACTION: callers see CrudRepository methods, not HTTP.
 */
public abstract class SupabaseRestRepository<T> implements CrudRepository<T> {
    private final SupabaseClient client;
    private final String table;
    private final String order;
    private final JsonCodec<T> codec;

    protected SupabaseRestRepository(SupabaseClient client, String table, String order, JsonCodec<T> codec) {
        this.client = client;
        this.table = table;
        this.order = order;
        this.codec = codec;
    }

    @Override
    public void findAll(String userId, Consumer<List<T>> onOk, Consumer<RepoError> onError) {
        // RLS already limits rows to this user; the filter just makes the intent explicit.
        String path = "/rest/v1/" + table + "?select=*&user_id=eq." + userId + "&order=" + order;
        client.rest("GET", path, null, null, body -> onOk.accept(decodeAll(body)), onError);
    }

    @Override
    public void insert(T item, Consumer<T> onOk, Consumer<RepoError> onError) {
        client.rest("POST", "/rest/v1/" + table, codec.toJson(item).stringify(), "return=representation",
                body -> onOk.accept(first(body, item)), onError);
    }

    @Override
    public void update(T item, Consumer<T> onOk, Consumer<RepoError> onError) {
        String path = "/rest/v1/" + table + "?id=eq." + codec.idOf(item);
        client.rest("PATCH", path, codec.toJson(item).stringify(), "return=representation", body -> {
            if (Json.parse(body).size() == 0) {
                onError.accept(RepoError.of("That item no longer exists. Pull down to refresh."));
            } else {
                onOk.accept(first(body, item));
            }
        }, onError);
    }

    @Override
    public void delete(String id, Runnable onOk, Consumer<RepoError> onError) {
        client.rest("DELETE", "/rest/v1/" + table + "?id=eq." + id, null, null, body -> onOk.run(), onError);
    }

    private List<T> decodeAll(String body) {
        List<T> items = new ArrayList<>();
        for (Json row : Json.parse(body).items()) {
            items.add(codec.fromJson(row));
        }
        return items;
    }

    private T first(String body, T fallback) {
        Json rows = Json.parse(body);
        return rows.size() > 0 ? codec.fromJson(rows.at(0)) : fallback;
    }
}
