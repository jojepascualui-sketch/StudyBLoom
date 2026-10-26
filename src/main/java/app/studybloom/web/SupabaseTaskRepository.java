package app.studybloom.web;

import app.studybloom.model.Task;
import app.studybloom.repo.TaskRepository;

/** INHERITANCE: all the HTTP work is in SupabaseRestRepository; this class picks the tasks table. */
public class SupabaseTaskRepository extends SupabaseRestRepository<Task> implements TaskRepository {
    public SupabaseTaskRepository(SupabaseClient client) {
        super(client, "tasks", "due_date.asc", new TaskCodec());
    }
}
