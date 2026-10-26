package app.studybloom.ui.components;

import app.studybloom.model.Task;
import app.studybloom.service.DueText;
import app.studybloom.ui.Component;
import app.studybloom.ui.Dom;
import app.studybloom.ui.Icons;
import org.teavm.jso.dom.html.HTMLElement;

/** One assignment row: the round check, the title, the subject tag and the due date. */
public class TaskCard extends Component {

    public interface Listener {
        void onToggle(TaskCard card);

        void onOpen(TaskCard card);
    }

    private final Task task;
    private final DueText due;
    private final Listener listener;
    private HTMLElement check;
    private HTMLElement dueLabel;

    public TaskCard(Task task, DueText due, Listener listener) {
        this.task = task;
        this.due = due;
        this.listener = listener;
    }

    public Task getTask() {
        return task;
    }

    @Override
    protected HTMLElement build() {
        HTMLElement card = Dom.el("article", "task" + (task.isCompleted() ? " done" : ""));
        card.setAttribute("data-id", task.getId());

        check = Dom.el("button", "check" + (task.isCompleted() ? " on" : ""));
        check.setAttribute("type", "button");
        check.setAttribute("role", "checkbox");
        check.setAttribute("aria-checked", task.isCompleted() ? "true" : "false");
        check.setAttribute("aria-label", (task.isCompleted() ? "Mark not done: " : "Mark complete: ") + task.getTitle());
        check.setInnerHTML("<span class=\"check-dot\">" + Icons.CHECK + "</span>");
        Dom.onClick(check, () -> listener.onToggle(this));

        HTMLElement body = Dom.div("task-body");
        body.setAttribute("role", "button");
        body.setAttribute("tabindex", "0");
        body.setAttribute("aria-label", "Details for " + task.getTitle());
        Dom.onActivate(body, () -> listener.onOpen(this));

        HTMLElement meta = Dom.div("meta");
        if (task.hasSubject()) {
            meta.appendChild(Dom.text("span", SubjectColors.tagClass(task.getSubject()), task.getSubject()));
        }
        dueLabel = Dom.text("span", dueClass(due.getStyle()), due.getText());
        meta.appendChild(dueLabel);

        Dom.append(body, Dom.text("div", "t", task.getTitle()), meta);
        Dom.append(card, check, body);
        return card;
    }

    /** The completion moment, in place: circle fills, title strikes through, due text changes. */
    public void showCompleted(String text) {
        HTMLElement card = getElement();
        card.getClassList().add("done");
        card.getClassList().add("just-done");
        check.getClassList().add("on");
        check.setAttribute("aria-checked", "true");
        dueLabel.setClassName(dueClass(DueText.Style.DONE));
        dueLabel.setTextContent(text);
    }

    private static String dueClass(DueText.Style style) {
        switch (style) {
            case OVERDUE:
                return "due over";
            case TODAY:
                return "due today";
            default:
                return "due";
        }
    }
}
