package app.studybloom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.studybloom.fakes.CountingIds;
import app.studybloom.fakes.FakeSubscriptionRepository;
import app.studybloom.fakes.FixedClock;
import app.studybloom.fakes.InMemoryTaskRepository;
import app.studybloom.model.PlanType;
import app.studybloom.model.Task;
import app.studybloom.repo.RepoError;
import app.studybloom.time.TaskGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskServiceTest {
    private FixedClock clock;
    private InMemoryTaskRepository repository;
    private FakeSubscriptionRepository plans;
    private SubscriptionService subscriptions;
    private TaskService service;

    @BeforeEach
    void setUp() {
        clock = new FixedClock();
        repository = new InMemoryTaskRepository();
        plans = new FakeSubscriptionRepository();
        subscriptions = new SubscriptionService(plans);
        service = new TaskService(repository, subscriptions, clock, new CountingIds());
        service.setUserId("bea");
    }

    private Task add(String title, long dueAt) {
        AtomicReference<Task> created = new AtomicReference<>();
        service.addTask(title, dueAt, null, null, created::set, error -> {
            throw new AssertionError(error.toString());
        });
        return created.get();
    }

    private ValidationError addExpectingError(String title, long dueAt) {
        AtomicReference<ValidationError> problem = new AtomicReference<>();
        service.addTask(title, dueAt, null, null, task -> {
            throw new AssertionError("expected an error");
        }, problem::set);
        return problem.get();
    }

    @Test
    void addTaskSavesItAndShowsItAsUnfinished() {
        Task task = add("  Lab report: titration ", clock.at(1, 23, 59));
        assertEquals("Lab report: titration", task.getTitle());
        assertEquals("bea", task.getUserId());
        assertEquals(1, repository.rows().size());
        assertEquals(1, service.countUnfinished());
        assertEquals(task.getId(), service.getUnfinished().get(0).getId());
    }

    @Test
    void titleAndDueDateAreRequired() {
        assertEquals("title", addExpectingError("   ", clock.at(1, 9, 0)).getField());
        assertEquals("due", addExpectingError("Essay", 0).getField());
        assertEquals("title", addExpectingError("x".repeat(201), clock.at(1, 9, 0)).getField());
        assertTrue(repository.rows().isEmpty());
    }

    @Test
    void freePlanStopsAtFifteenUnfinishedTasks() {
        for (int i = 0; i < 15; i++) {
            add("Task " + i, clock.at(1, 9, 0));
        }
        ValidationError problem = addExpectingError("Task 16", clock.at(1, 9, 0));
        assertNotNull(problem);
        assertTrue(problem.isPlanLimit());
        assertEquals(15, repository.rows().size());
    }

    @Test
    void completingATaskFreesUpASlotOnFree() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            tasks.add(add("Task " + i, clock.at(1, 9, 0)));
        }
        service.complete(tasks.get(0), () -> { }, error -> { });
        assertNotNull(add("Task 16", clock.at(2, 9, 0)));
    }

    @Test
    void proPlanHasNoTaskLimitAndKeepsSubjects() {
        plans.setPlan(PlanType.PRO);
        subscriptions.load("bea", () -> { }, error -> { });
        for (int i = 0; i < 20; i++) {
            add("Task " + i, clock.at(1, 9, 0));
        }
        AtomicReference<Task> tagged = new AtomicReference<>();
        service.addTask("Problem set", clock.at(3, 9, 0), "Calculus 1", null, tagged::set, e -> { });
        assertEquals(21, service.countUnfinished());
        assertEquals("Calculus 1", tagged.get().getSubject());
    }

    @Test
    void freePlanDropsTheSubject() {
        AtomicReference<Task> task = new AtomicReference<>();
        service.addTask("Problem set", clock.at(3, 9, 0), "Calculus 1", "page 40", task::set, e -> { });
        assertNull(task.get().getSubject());
        assertEquals("page 40", task.get().getDescription());
    }

    @Test
    void completeAndUndo() {
        Task task = add("Essay", clock.at(0, 23, 59));
        service.complete(task, () -> { }, error -> { });
        assertTrue(repository.rows().get(0).isCompleted());
        assertEquals(clock.nowMillis(), repository.rows().get(0).getCompletedAt());
        assertEquals(0, service.countUnfinished());
        assertEquals(1, service.getCompleted().size());

        service.undoComplete(task, () -> { }, error -> { });
        assertFalse(repository.rows().get(0).isCompleted());
        assertEquals(1, service.countUnfinished());
    }

    @Test
    void failedSaveRevertsTheCompletion() {
        Task task = add("Essay", clock.at(0, 23, 59));
        repository.failNext(RepoError.of("Server said no"));
        AtomicReference<String> error = new AtomicReference<>();
        service.complete(task, () -> { }, error::set);
        assertEquals("Server said no", error.get());
        assertFalse(service.find(task.getId()).isCompleted());
    }

    @Test
    void unfinishedTasksAreSortedByDueDate() {
        add("Later one", clock.at(10, 9, 0));
        add("Sooner one", clock.at(1, 9, 0));
        add("Middle one", clock.at(3, 9, 0));
        List<Task> unfinished = service.getUnfinished();
        assertEquals("Sooner one", unfinished.get(0).getTitle());
        assertEquals("Middle one", unfinished.get(1).getTitle());
        assertEquals("Later one", unfinished.get(2).getTitle());
    }

    @Test
    void groupsMatchTheMockup() {
        // Today is Monday Oct 26, 10:00.
        add("Lab report: titration", clock.at(-2, 23, 59));      // Sat Oct 24 -> Overdue
        add("Reflection essay, 500 words", clock.at(0, 23, 59)); // today      -> Today
        add("Problem set 4, limits", clock.at(3, 23, 59));       // Thu Oct 29 -> This week
        add("Group report outline", clock.at(4, 23, 59));        // Fri Oct 30 -> This week
        add("Final project", clock.at(14, 23, 59));              // next month -> Later

        Map<TaskGroup, List<Task>> groups = service.groupUnfinished();
        assertEquals(List.of(TaskGroup.OVERDUE, TaskGroup.TODAY, TaskGroup.THIS_WEEK, TaskGroup.LATER),
                new ArrayList<>(groups.keySet()));
        assertEquals(2, groups.get(TaskGroup.THIS_WEEK).size());
    }

    @Test
    void dueTextsMatchTheMockup() {
        Task overdue = add("Lab report", clock.at(-2, 23, 59));
        Task today = add("Essay", clock.at(0, 23, 59));
        Task thursday = add("Problem set", clock.at(3, 23, 59));

        assertEquals("Was due Sat, Oct 24", service.describeDue(overdue).getText());
        assertEquals(DueText.Style.OVERDUE, service.describeDue(overdue).getStyle());
        assertEquals("Due 11:59 PM", service.describeDue(today).getText());
        assertEquals(DueText.Style.TODAY, service.describeDue(today).getStyle());
        assertEquals("Thu, Oct 29", service.describeDue(thursday).getText());

        service.complete(today, () -> { }, e -> { });
        assertEquals("Done just now", service.describeDue(service.find(today.getId())).getText());
    }

    @Test
    void refreshLoadsFromTheRepository() {
        add("Essay", clock.at(1, 9, 0));
        TaskService fresh = new TaskService(repository, subscriptions, clock, new CountingIds());
        fresh.setUserId("bea");
        fresh.refresh(() -> { }, e -> { });
        assertEquals(1, fresh.countUnfinished());
    }

    @Test
    void deleteRemovesTheTask() {
        Task task = add("Essay", clock.at(1, 9, 0));
        service.delete(task, () -> { }, e -> { });
        assertEquals(0, service.countUnfinished());
        assertTrue(repository.rows().isEmpty());
    }
}
