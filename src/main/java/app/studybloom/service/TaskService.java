package app.studybloom.service;

import app.studybloom.model.Feature;
import app.studybloom.model.Task;
import app.studybloom.repo.IdGenerator;
import app.studybloom.repo.RepoError;
import app.studybloom.repo.TaskRepository;
import app.studybloom.time.AppClock;
import app.studybloom.time.DateTexts;
import app.studybloom.time.LocalDay;
import app.studybloom.time.TaskGroup;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The MVP logic: add a task, set its due date, complete it, list what is left.
 *
 * ENCAPSULATION: the task list is private. Screens get copies and change tasks
 * only by calling these methods, which also save to the repository.
 * ABSTRACTION: storage is a {@link TaskRepository}; this class never knows
 * whether that is Supabase, the offline cache, or a test fake.
 */
public class TaskService {
    public static final int MAX_TITLE_LENGTH = 200;

    private final TaskRepository repository;
    private final SubscriptionService subscriptions;
    private final AppClock clock;
    private final IdGenerator ids;
    private final List<Task> tasks = new ArrayList<>();
    private String userId;

    public TaskService(TaskRepository repository, SubscriptionService subscriptions, AppClock clock, IdGenerator ids) {
        this.repository = repository;
        this.subscriptions = subscriptions;
        this.clock = clock;
        this.ids = ids;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        tasks.clear();
    }

    public void refresh(Runnable onDone, Consumer<String> onError) {
        repository.findAll(userId, loaded -> {
            tasks.clear();
            tasks.addAll(loaded);
            onDone.run();
        }, error -> onError.accept(error.getMessage()));
    }

    // ---- reading -------------------------------------------------------

    /** Unfinished tasks, soonest due first. */
    public List<Task> getUnfinished() {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                result.add(task);
            }
        }
        result.sort(Comparator.comparingLong(Task::getDueAt));
        return result;
    }

    /** Completed tasks, most recently finished first. */
    public List<Task> getCompleted() {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (task.isCompleted()) {
                result.add(task);
            }
        }
        result.sort((a, b) -> Long.compare(b.getCompletedAt(), a.getCompletedAt()));
        return result;
    }

    public List<Task> getAll() {
        return new ArrayList<>(tasks);
    }

    public int countUnfinished() {
        int count = 0;
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    /** Unfinished tasks under Overdue / Today / This week / Later. Empty groups are left out. */
    public Map<TaskGroup, List<Task>> groupUnfinished() {
        Map<TaskGroup, List<Task>> groups = new LinkedHashMap<>();
        for (TaskGroup group : TaskGroup.values()) {
            groups.put(group, new ArrayList<>());
        }
        for (Task task : getUnfinished()) {
            groups.get(groupOf(task)).add(task);
        }
        groups.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return groups;
    }

    public TaskGroup groupOf(Task task) {
        long now = clock.nowMillis();
        if (task.getDueAt() < now) {
            return TaskGroup.OVERDUE;
        }
        LocalDay today = clock.today();
        LocalDay due = LocalDay.of(task.getDueAt(), clock);
        if (due.equals(today)) {
            return TaskGroup.TODAY;
        }
        if (!due.isAfter(today.endOfWeek())) {
            return TaskGroup.THIS_WEEK;
        }
        return TaskGroup.LATER;
    }

    /** "Was due Sat, Oct 24", "Due 11:59 PM", "Thu, Oct 29", "Done just now". */
    public DueText describeDue(Task task) {
        LocalDay today = clock.today();
        if (task.isCompleted()) {
            long ago = clock.nowMillis() - task.getCompletedAt();
            if (ago < 5 * 60_000L) {
                return new DueText("Done just now", DueText.Style.DONE);
            }
            LocalDay doneDay = LocalDay.of(task.getCompletedAt(), clock);
            String when = doneDay.equals(today) ? "today" : DateTexts.shortDate(doneDay);
            return new DueText("Done " + when, DueText.Style.DONE);
        }
        LocalDay due = LocalDay.of(task.getDueAt(), clock);
        if (task.getDueAt() < clock.nowMillis()) {
            String when = due.equals(today) ? DateTexts.time(task.getDueAt(), clock) : DateTexts.shortDate(due);
            return new DueText("Was due " + when, DueText.Style.OVERDUE);
        }
        if (due.equals(today)) {
            return new DueText("Due " + DateTexts.time(task.getDueAt(), clock), DueText.Style.TODAY);
        }
        if (due.equals(today.plusDays(1))) {
            return new DueText("Tomorrow, " + DateTexts.time(task.getDueAt(), clock), DueText.Style.NORMAL);
        }
        return new DueText(DateTexts.shortDate(due), DueText.Style.NORMAL);
    }

    /** Subjects already used, for the chips in the Add sheet. */
    public List<String> knownSubjects() {
        Set<String> subjects = new LinkedHashSet<>();
        for (Task task : tasks) {
            if (task.hasSubject()) {
                subjects.add(task.getSubject());
            }
        }
        return new ArrayList<>(subjects);
    }

    // ---- changing ------------------------------------------------------

    /** Returns null when the input is fine, otherwise what to fix. */
    public ValidationError validate(String title, long dueAt) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            return new ValidationError("title", "Give the task a name, like “Read chapter 6”.");
        }
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            return new ValidationError("title", "Keep the name under " + MAX_TITLE_LENGTH + " characters. Put details in Notes.");
        }
        if (dueAt <= 0) {
            return new ValidationError("due", "Pick a due date so StudyBloom can remind you.");
        }
        return null;
    }

    public void addTask(String title, long dueAt, String subject, String notes,
                        Consumer<Task> onOk, Consumer<ValidationError> onError) {
        ValidationError problem = validate(title, dueAt);
        if (problem != null) {
            onError.accept(problem);
            return;
        }
        if (!subscriptions.canAddTask(countUnfinished())) {
            onError.accept(new ValidationError(ValidationError.FORM, subscriptions.limitMessage(), true));
            return;
        }
        Task task = new Task(ids.newId(), userId, title, dueAt);
        task.setCreatedAt(clock.nowMillis());
        task.setDescription(notes);
        if (subscriptions.isAvailable(Feature.SUBJECT_TAGS)) {
            task.setSubject(subject);
        }
        repository.insert(task, saved -> {
            tasks.add(saved);
            onOk.accept(saved);
        }, error -> onError.accept(fromRepo(error)));
    }

    /** Completes immediately in memory (so the UI can celebrate), then saves. */
    public void complete(Task task, Runnable onOk, Consumer<String> onError) {
        Task local = find(task.getId());
        if (local == null || local.isCompleted()) {
            return;
        }
        local.markCompleted(clock.nowMillis());
        save(local, onOk, error -> {
            local.markUnfinished();
            onError.accept(error);
        });
    }

    /** The Undo button on the toast. */
    public void undoComplete(Task task, Runnable onOk, Consumer<String> onError) {
        Task local = find(task.getId());
        if (local == null || !local.isCompleted()) {
            return;
        }
        long completedAt = local.getCompletedAt();
        local.markUnfinished();
        save(local, onOk, error -> {
            local.markCompleted(completedAt);
            onError.accept(error);
        });
    }

    public void delete(Task task, Runnable onOk, Consumer<String> onError) {
        repository.delete(task.getId(), () -> {
            tasks.removeIf(t -> t.getId().equals(task.getId()));
            onOk.run();
        }, error -> onError.accept(error.getMessage()));
    }

    public Task find(String id) {
        for (Task task : tasks) {
            if (task.getId().equals(id)) {
                return task;
            }
        }
        return null;
    }

    private void save(Task task, Runnable onOk, Consumer<String> onError) {
        repository.update(task.copy(), saved -> onOk.run(), error -> onError.accept(error.getMessage()));
    }

    private ValidationError fromRepo(RepoError error) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (message.toLowerCase().contains("limit")) {
            return new ValidationError(ValidationError.FORM, subscriptions.limitMessage(), true);
        }
        return new ValidationError(ValidationError.FORM, message);
    }
}
