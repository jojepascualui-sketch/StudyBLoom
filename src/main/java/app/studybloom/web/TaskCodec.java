package app.studybloom.web;

import app.studybloom.model.Task;

/** Maps Task to the columns of the tasks table. */
public class TaskCodec implements JsonCodec<Task> {
    @Override
    public Json toJson(Task task) {
        Json json = Json.object()
                .put("id", task.getId())
                .put("user_id", task.getUserId())
                .put("title", task.getTitle())
                .put("subject", task.getSubject())
                .put("description", task.getDescription())
                .putTimestamp("due_date", task.getDueAt())
                .put("is_completed", task.isCompleted())
                .putTimestamp("completed_at", task.isCompleted() ? task.getCompletedAt() : 0);
        if (task.getCreatedAt() > 0) {
            // Left out otherwise, so the database default now() applies.
            json.putTimestamp("created_at", task.getCreatedAt());
        }
        return json;
    }

    @Override
    public Task fromJson(Json json) {
        Task task = new Task();
        task.setId(json.getString("id"));
        task.setUserId(json.getString("user_id"));
        task.setTitle(json.getString("title"));
        task.setSubject(json.getString("subject"));
        task.setDescription(json.getString("description"));
        task.setDueAt(json.getTimestamp("due_date"));
        task.setCompleted(json.getBoolean("is_completed"));
        task.setCompletedAt(json.getTimestamp("completed_at"));
        task.setCreatedAt(json.getTimestamp("created_at"));
        return task;
    }

    @Override
    public String idOf(Task task) {
        return task.getId();
    }
}
