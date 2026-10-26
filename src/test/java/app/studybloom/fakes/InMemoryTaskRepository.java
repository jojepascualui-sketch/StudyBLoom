package app.studybloom.fakes;

import app.studybloom.model.Task;
import app.studybloom.repo.RepoError;
import app.studybloom.repo.TaskRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** POLYMORPHISM in tests: TaskService runs against this instead of Supabase. */
public class InMemoryTaskRepository implements TaskRepository {
    private final List<Task> rows = new ArrayList<>();
    private RepoError nextError;

    /** Makes the next call fail with this error. */
    public void failNext(RepoError error) {
        nextError = error;
    }

    public List<Task> rows() {
        return rows;
    }

    @Override
    public void findAll(String userId, Consumer<List<Task>> onOk, Consumer<RepoError> onError) {
        if (failed(onError)) {
            return;
        }
        List<Task> copies = new ArrayList<>();
        for (Task row : rows) {
            if (userId == null || userId.equals(row.getUserId())) {
                copies.add(row.copy());
            }
        }
        onOk.accept(copies);
    }

    @Override
    public void insert(Task item, Consumer<Task> onOk, Consumer<RepoError> onError) {
        if (failed(onError)) {
            return;
        }
        rows.add(item.copy());
        onOk.accept(item.copy());
    }

    @Override
    public void update(Task item, Consumer<Task> onOk, Consumer<RepoError> onError) {
        if (failed(onError)) {
            return;
        }
        rows.replaceAll(row -> row.getId().equals(item.getId()) ? item.copy() : row);
        onOk.accept(item.copy());
    }

    @Override
    public void delete(String id, Runnable onOk, Consumer<RepoError> onError) {
        if (failed(onError)) {
            return;
        }
        rows.removeIf(row -> row.getId().equals(id));
        onOk.run();
    }

    private boolean failed(Consumer<RepoError> onError) {
        if (nextError == null) {
            return false;
        }
        RepoError error = nextError;
        nextError = null;
        onError.accept(error);
        return true;
    }
}
