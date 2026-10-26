package app.studybloom.repo;

import app.studybloom.model.Task;

/**
 * ABSTRACTION: TaskService depends on this interface only.
 * POLYMORPHISM: at runtime it is a SupabaseTaskRepository (wrapped in the
 * offline cache), a LocalTaskRepository in local demo mode, or an in-memory
 * fake in the unit tests.
 */
public interface TaskRepository extends CrudRepository<Task> {
}
