package app.studybloom.web;

import app.studybloom.model.Task;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.TaskRepository;

/** POLYMORPHISM: a TaskRepository that never leaves the device (local demo mode). */
public class LocalTaskRepository extends LocalListRepository<Task> implements TaskRepository {
    public LocalTaskRepository(KeyValueStore store) {
        super(store, "sb.local.tasks", new TaskCodec());
    }
}
