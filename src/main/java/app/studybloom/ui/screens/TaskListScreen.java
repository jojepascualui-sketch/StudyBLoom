package app.studybloom.ui.screens;

import app.studybloom.model.Student;
import app.studybloom.model.Task;
import app.studybloom.service.TaskService;
import app.studybloom.time.DateTexts;
import app.studybloom.time.LocalDay;
import app.studybloom.time.TaskGroup;
import app.studybloom.ui.App;
import app.studybloom.ui.Dom;
import app.studybloom.ui.Icons;
import app.studybloom.ui.NavTab;
import app.studybloom.ui.Screen;
import app.studybloom.ui.components.BloomMeter;
import app.studybloom.ui.components.PetalBurst;
import app.studybloom.ui.components.SubjectColors;
import app.studybloom.ui.components.TaskCard;
import app.studybloom.ui.components.Toast;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screen 2 (My tasks) and screen 4 (the completion moment).
 * Unfinished tasks are grouped Overdue / Today / This week / Later.
 */
public class TaskListScreen extends Screen implements TaskCard.Listener {
    private enum Filter { TODO, DONE, ALL }

    private final Map<String, TaskCard> cards = new LinkedHashMap<>();
    private Filter filter = Filter.TODO;
    private BloomMeter bloom;
    private boolean loading;
    private String highlightId;

    public TaskListScreen(App app) {
        super(app);
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    /** Briefly highlights a just-added task on the next render. */
    public void highlight(String taskId) {
        highlightId = taskId;
    }

    @Override
    public NavTab getTab() {
        return NavTab.TASKS;
    }

    @Override
    public String getTitle() {
        return "My tasks";
    }

    @Override
    protected HTMLElement build() {
        TaskService tasks = app().services().getTasks();
        Student student = app().services().getAuth().getCurrentStudent();
        cards.clear();

        HTMLElement page = Dom.div("tasks-screen");

        HTMLElement header = Dom.el("header", "home-head");
        HTMLElement hello = Dom.div("");
        Dom.append(hello,
                Dom.text("p", "h-sub", DateTexts.longDate(app().services().getClock().today())),
                Dom.text("h1", "h-title", "Hi, " + (student == null ? "there" : student.getFirstName())));
        HTMLElement avatar = Dom.button("avatar", student == null ? "?" : student.getInitial(),
                () -> app().navigate(NavTab.ME));
        avatar.setAttribute("aria-label", "Me and settings");
        Dom.append(header, hello, avatar);

        bloom = new BloomMeter(app().services().getBloom().compute(tasks.getAll()));

        Dom.append(page, header, bloom.render(), buildFilter());

        HTMLElement list = Dom.el("section", "task-list");
        list.setAttribute("aria-live", "polite");
        if (loading) {
            list.appendChild(Dom.text("p", "empty", "Loading your tasks…"));
        } else if (filter == Filter.DONE) {
            addCards(list, null, tasks.getCompleted());
            if (tasks.getCompleted().isEmpty()) {
                list.appendChild(Dom.text("p", "empty", "Nothing finished yet. Tap a circle when you're done with a task."));
            }
        } else {
            Map<TaskGroup, List<Task>> groups = tasks.groupUnfinished();
            for (Map.Entry<TaskGroup, List<Task>> group : groups.entrySet()) {
                addCards(list, group.getKey().getLabel(), group.getValue());
            }
            if (filter == Filter.ALL && !tasks.getCompleted().isEmpty()) {
                addCards(list, "Done", tasks.getCompleted());
            }
            if (groups.isEmpty() && (filter == Filter.TODO || tasks.getCompleted().isEmpty())) {
                list.appendChild(emptyState());
            }
        }
        page.appendChild(list);

        HTMLElement fab = Dom.el("button", "fab");
        fab.setAttribute("type", "button");
        fab.setAttribute("aria-label", "Add assignment");
        fab.setInnerHTML(Icons.PLUS);
        Dom.onClick(fab, this::openAddSheet);
        page.appendChild(fab);
        return page;
    }

    private HTMLElement buildFilter() {
        HTMLElement seg = Dom.div("seg");
        seg.setAttribute("role", "tablist");
        seg.setAttribute("aria-label", "Show");
        addSegment(seg, "To do", Filter.TODO);
        addSegment(seg, "Done", Filter.DONE);
        addSegment(seg, "All", Filter.ALL);
        return seg;
    }

    private void addSegment(HTMLElement seg, String label, Filter value) {
        HTMLElement button = Dom.button(filter == value ? "on" : "", label, () -> {
            app().toast().flush();
            filter = value;
            app().rerender();
        });
        button.setAttribute("role", "tab");
        button.setAttribute("aria-selected", filter == value ? "true" : "false");
        seg.appendChild(button);
    }

    private void addCards(HTMLElement list, String label, List<Task> group) {
        if (group.isEmpty()) {
            return;
        }
        if (label != null) {
            list.appendChild(Dom.text("h2", "group-label", label));
        }
        TaskService tasks = app().services().getTasks();
        for (Task task : group) {
            TaskCard card = new TaskCard(task, tasks.describeDue(task), this);
            HTMLElement element = card.render();
            if (task.getId().equals(highlightId)) {
                element.getClassList().add("just-added");
                highlightId = null;
            }
            cards.put(task.getId(), card);
            list.appendChild(element);
        }
    }

    private HTMLElement emptyState() {
        HTMLElement box = Dom.div("empty-state");
        Dom.append(box, Dom.svg("div", "", Icons.logo(56)),
                Dom.text("p", "empty-title", "No assignments waiting"),
                Dom.text("p", "h-sub", "Heard about one in class or the group chat? Tap + to add it before you forget."));
        return box;
    }

    private void openAddSheet() {
        app().toast().flush();
        AddTaskScreen add = new AddTaskScreen(app());
        app().openSheet(add.render(), "New assignment");
        add.onShown();
    }

    // ---- TaskCard.Listener ---------------------------------------------

    @Override
    public void onToggle(TaskCard clicked) {
        Task task = clicked.getTask();
        TaskService tasks = app().services().getTasks();
        if (task.isCompleted()) {
            tasks.undoComplete(task, () -> { }, error -> app().toast().show(error));
            app().rerender();
            app().toast().show("Moved back to To do.");
            return;
        }
        // Finish any earlier Undo toast first; that re-renders, so look the card up again.
        app().toast().flush();
        TaskCard card = cards.get(task.getId());
        if (card == null) {
            return;
        }
        tasks.complete(task, () -> { }, error -> {
            app().toast().show("Couldn't save that: " + error);
            app().rerender();
        });
        card.showCompleted(tasks.describeDue(tasks.find(task.getId())).getText());
        PetalBurst.play(card.getElement());
        bloom.update(app().services().getBloom().compute(tasks.getAll()));
        app().toast().show("Marked complete. Nice one!", "Undo", () -> {
            tasks.undoComplete(task, () -> { }, error -> app().toast().show(error));
            app().rerender();
        }, Toast.UNDO_MS, this::rerenderIfShown);
    }

    @Override
    public void onOpen(TaskCard card) {
        app().toast().flush();
        Task task = app().services().getTasks().find(card.getTask().getId());
        if (task != null) {
            app().openSheet(detailSheet(task), task.getTitle());
        }
    }

    private void rerenderIfShown() {
        if (app().getCurrent() == this) {
            app().rerender();
        }
    }

    /** Extension (not in the mockups): tap a task to see its notes or delete it. */
    private HTMLElement detailSheet(Task task) {
        TaskService tasks = app().services().getTasks();
        HTMLElement box = Dom.div("detail");
        box.appendChild(Dom.text("h2", "h-title sheet-title", task.getTitle()));
        HTMLElement meta = Dom.div("meta");
        if (task.hasSubject()) {
            meta.appendChild(Dom.text("span", SubjectColors.tagClass(task.getSubject()), task.getSubject()));
        }
        LocalDay dueDay = LocalDay.of(task.getDueAt(), app().services().getClock());
        String dueLine = DateTexts.shortDate(dueDay) + ", " + DateTexts.time(task.getDueAt(), app().services().getClock());
        meta.appendChild(Dom.text("span", "due", "Due " + dueLine));
        box.appendChild(meta);
        box.appendChild(Dom.text("p", "detail-notes" + (task.hasDescription() ? "" : " muted"),
                task.hasDescription() ? task.getDescription() : "No notes."));

        HTMLElement toggle = Dom.button("btn btn-primary", task.isCompleted() ? "Mark not done" : "Mark complete", () -> {
            app().closeSheet();
            TaskCard card = cards.get(task.getId());
            if (card != null) {
                onToggle(card);
            }
        });
        HTMLElement delete = Dom.button("btn btn-ghost btn-danger", "Delete task", () -> {
            app().closeSheet();
            tasks.delete(task, () -> {
                app().rerender();
                app().toast().show("Task deleted.");
            }, error -> app().toast().show(error));
        });
        Dom.append(box, toggle, Dom.div("spacer-10"), delete);
        return box;
    }
}
