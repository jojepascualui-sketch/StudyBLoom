package app.studybloom.web;

import app.studybloom.model.Task;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.RepoError;
import app.studybloom.repo.TaskRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Offline support, as a decorator around SupabaseTaskRepository.
 *
 * POLYMORPHISM: it is itself a TaskRepository, so TaskService uses it without
 * knowing it exists. When the network is down it serves the last saved task
 * list and queues changes; {@link #flush(Runnable)} replays them in order once
 * the browser is back online.
 */
public class OfflineTaskRepository implements TaskRepository {
    private static final String CACHE_KEY = "sb.tasks.cache";
    private static final String QUEUE_KEY = "sb.tasks.queue";

    private final TaskRepository remote;
    private final KeyValueStore store;
    private final TaskCodec codec = new TaskCodec();
    private boolean flushing;
    private String lastSyncProblem;

    public OfflineTaskRepository(TaskRepository remote, KeyValueStore store) {
        this.remote = remote;
        this.store = store;
    }

    @Override
    public void findAll(String userId, Consumer<List<Task>> onOk, Consumer<RepoError> onError) {
        flush(() -> remote.findAll(userId, tasks -> {
            if (pendingCount() == 0) {
                writeCache(tasks);
                onOk.accept(tasks);
            } else {
                // Still offline for some queued changes: the cache already includes them.
                onOk.accept(readCache());
            }
        }, error -> {
            if (error.isOffline()) {
                onOk.accept(readCache());
            } else {
                onError.accept(error);
            }
        }));
    }

    @Override
    public void insert(Task task, Consumer<Task> onOk, Consumer<RepoError> onError) {
        write("insert", task, onOk, onError);
    }

    @Override
    public void update(Task task, Consumer<Task> onOk, Consumer<RepoError> onError) {
        write("update", task, onOk, onError);
    }

    @Override
    public void delete(String id, Runnable onOk, Consumer<RepoError> onError) {
        if (pendingCount() > 0) {
            queueDelete(id);
            onOk.run();
            return;
        }
        remote.delete(id, () -> {
            removeFromCache(id);
            onOk.run();
        }, error -> {
            if (error.isOffline()) {
                queueDelete(id);
                onOk.run();
            } else {
                onError.accept(error);
            }
        });
    }

    public int pendingCount() {
        String queue = store.get(QUEUE_KEY);
        return queue == null ? 0 : Json.parse(queue).size();
    }

    /** A queued change the server refused (for example the Free limit), or null. */
    public String takeLastSyncProblem() {
        String problem = lastSyncProblem;
        lastSyncProblem = null;
        return problem;
    }

    /** Sends queued changes one by one, oldest first, then runs {@code then}. */
    public void flush(Runnable then) {
        List<Json> queue = readQueue();
        if (queue.isEmpty() || flushing) {
            then.run();
            return;
        }
        flushing = true;
        Json op = queue.get(0);
        Runnable next = () -> {
            List<Json> rest = readQueue();
            if (!rest.isEmpty()) {
                rest.remove(0);
            }
            writeQueue(rest);
            flushing = false;
            flush(then);
        };
        Consumer<RepoError> failed = error -> {
            if (error.isOffline()) {
                flushing = false;
                then.run();
            } else {
                lastSyncProblem = error.getMessage();
                next.run();
            }
        };
        String kind = op.getString("op");
        if ("delete".equals(kind)) {
            remote.delete(op.getString("id"), next, failed);
        } else {
            Task task = codec.fromJson(op.get("task"));
            if ("insert".equals(kind)) {
                remote.insert(task, saved -> next.run(), failed);
            } else {
                remote.update(task, saved -> next.run(), failed);
            }
        }
    }

    /** Called on sign out so the next student never sees these tasks. */
    public void clear() {
        store.remove(CACHE_KEY);
        store.remove(QUEUE_KEY);
    }

    // ---- internals -----------------------------------------------------

    private void write(String kind, Task task, Consumer<Task> onOk, Consumer<RepoError> onError) {
        if (pendingCount() > 0) {
            queue(kind, task);
            onOk.accept(task);
            return;
        }
        Consumer<Task> saved = result -> {
            putInCache(result);
            onOk.accept(result);
        };
        Consumer<RepoError> failed = error -> {
            if (error.isOffline()) {
                queue(kind, task);
                onOk.accept(task);
            } else {
                onError.accept(error);
            }
        };
        if ("insert".equals(kind)) {
            remote.insert(task, saved, failed);
        } else {
            remote.update(task, saved, failed);
        }
    }

    private void queue(String kind, Task task) {
        List<Json> queue = readQueue();
        queue.add(Json.object().put("op", kind).put("task", codec.toJson(task)));
        writeQueue(queue);
        putInCache(task);
    }

    private void queueDelete(String id) {
        List<Json> queue = readQueue();
        queue.add(Json.object().put("op", "delete").put("id", id));
        writeQueue(queue);
        removeFromCache(id);
    }

    private void putInCache(Task task) {
        List<Task> tasks = readCache();
        boolean replaced = false;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                tasks.set(i, task);
                replaced = true;
            }
        }
        if (!replaced) {
            tasks.add(task);
        }
        writeCache(tasks);
    }

    private void removeFromCache(String id) {
        List<Task> tasks = readCache();
        tasks.removeIf(t -> t.getId().equals(id));
        writeCache(tasks);
    }

    private List<Task> readCache() {
        List<Task> tasks = new ArrayList<>();
        String saved = store.get(CACHE_KEY);
        if (saved != null) {
            for (Json row : Json.parse(saved).items()) {
                tasks.add(codec.fromJson(row));
            }
        }
        return tasks;
    }

    private void writeCache(List<Task> tasks) {
        Json array = Json.array();
        for (Task task : tasks) {
            array.add(codec.toJson(task));
        }
        store.set(CACHE_KEY, array.stringify());
    }

    private List<Json> readQueue() {
        String saved = store.get(QUEUE_KEY);
        return saved == null ? new ArrayList<>() : Json.parse(saved).items();
    }

    private void writeQueue(List<Json> queue) {
        if (queue.isEmpty()) {
            store.remove(QUEUE_KEY);
            return;
        }
        Json array = Json.array();
        for (Json op : queue) {
            array.add(op);
        }
        store.set(QUEUE_KEY, array.stringify());
    }
}
