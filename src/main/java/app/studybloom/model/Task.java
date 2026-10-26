package app.studybloom.model;

/**
 * One school assignment.
 *
 * ENCAPSULATION: every field is private. Other classes read and change a task
 * only through getters, setters and the two state methods below, so a task can
 * never end up "completed" without a completion time.
 */
public class Task {
    private String id;
    private String userId;
    private String title;
    private String subject;
    private String description;
    private long dueAt;
    private boolean completed;
    private long completedAt;
    private long createdAt;

    public Task() {
    }

    public Task(String id, String userId, String title, long dueAt) {
        this.id = id;
        this.userId = userId;
        setTitle(title);
        this.dueAt = dueAt;
    }

    /** Marks the task done and remembers when, for the bloom meter. */
    public void markCompleted(long now) {
        completed = true;
        completedAt = now;
    }

    /** Undo for {@link #markCompleted(long)}. */
    public void markUnfinished() {
        completed = false;
        completedAt = 0;
    }

    public boolean hasSubject() {
        return subject != null && !subject.isEmpty();
    }

    public boolean hasDescription() {
        return description != null && !description.isEmpty();
    }

    public Task copy() {
        Task t = new Task();
        t.id = id;
        t.userId = userId;
        t.title = title;
        t.subject = subject;
        t.description = description;
        t.dueAt = dueAt;
        t.completed = completed;
        t.completedAt = completedAt;
        t.createdAt = createdAt;
        return t;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = blankToNull(subject);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = blankToNull(description);
    }

    public long getDueAt() {
        return dueAt;
    }

    public void setDueAt(long dueAt) {
        this.dueAt = dueAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
