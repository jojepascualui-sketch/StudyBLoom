package app.studybloom.ui.screens;

import app.studybloom.model.Feature;
import app.studybloom.service.ValidationError;
import app.studybloom.time.AppClock;
import app.studybloom.time.LocalDay;
import app.studybloom.ui.App;
import app.studybloom.ui.Dom;
import app.studybloom.ui.NavTab;
import app.studybloom.ui.Screen;
import app.studybloom.ui.components.ChipGroup;
import app.studybloom.ui.components.FormField;
import java.util.List;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLTextAreaElement;

/**
 * Screen 3: the "New assignment" bottom sheet. Only the title and due date are
 * required. Subject tags are Pro, so on Free the Subject row is hidden.
 */
public class AddTaskScreen extends Screen {
    private static final String TODAY = "Today";
    private static final String TOMORROW = "Tomorrow";
    private static final String NEXT_WEEK = "Next week";
    private static final String PICK = "Pick a date";

    private HTMLInputElement title;
    private HTMLTextAreaElement notes;
    private HTMLInputElement pickDate;
    private HTMLInputElement pickTime;
    private HTMLElement picker;
    private FormField titleField;
    private FormField dueField;
    private ChipGroup subjects;
    private ChipGroup dueChips;
    private HTMLElement formError;
    private HTMLElement submit;

    public AddTaskScreen(App app) {
        super(app);
    }

    @Override
    public NavTab getTab() {
        return NavTab.TASKS;
    }

    @Override
    public String getTitle() {
        return "New assignment";
    }

    @Override
    protected HTMLElement build() {
        HTMLElement form = Dom.el("form", "add-form");
        form.setAttribute("novalidate", "");
        form.addEventListener("submit", event -> {
            event.preventDefault();
            save();
        });

        form.appendChild(Dom.text("h2", "h-title sheet-title", "New assignment"));

        title = Dom.input("text", "input");
        title.setPlaceholder("Read chapter 6 and answer the guide");
        title.setAttribute("maxlength", "200");
        title.setAttribute("enterkeyhint", "done");
        titleField = new FormField("What’s the task?", title);
        form.appendChild(titleField.getElement());

        if (app().services().getSubscriptions().isAvailable(Feature.SUBJECT_TAGS)) {
            form.appendChild(buildSubjects());
        }

        dueChips = new ChipGroup("Due date", List.of(TODAY, TOMORROW, NEXT_WEEK, PICK), this::onDueChip);
        HTMLElement dueBox = Dom.div("");
        picker = Dom.div("date-picker");
        picker.setHidden(true);
        AppClock clock = app().services().getClock();
        pickDate = Dom.input("date", "input");
        pickDate.setAttribute("aria-label", "Due date");
        pickDate.setAttribute("min", clock.today().toIsoString());
        pickDate.setValue(clock.today().plusDays(2).toIsoString());
        pickTime = Dom.input("time", "input");
        pickTime.setAttribute("aria-label", "Due time");
        pickTime.setValue("23:59");
        Dom.append(picker, pickDate, pickTime);
        Dom.append(dueBox, dueChips.getElement(), picker);
        dueField = new FormField("Due date", dueBox);
        form.appendChild(dueField.getElement());

        notes = Dom.textarea("input area");
        notes.setPlaceholder("Where it was announced, page numbers…");
        notes.setAttribute("maxlength", "2000");
        form.appendChild(new FormField("Notes (optional)", notes).getElement());

        formError = Dom.el("p", "form-error");
        formError.setAttribute("role", "alert");
        formError.setHidden(true);

        submit = Dom.el("button", "btn btn-primary");
        submit.setAttribute("type", "submit");
        submit.setTextContent("Add assignment");
        Dom.append(form, formError, submit);
        return form;
    }

    @Override
    public void onShown() {
        title.focus();
    }

    private HTMLElement buildSubjects() {
        HTMLElement box = Dom.div("field");
        box.appendChild(Dom.text("p", "field-label", "Subject"));
        subjects = new ChipGroup("Subject", app().services().getTasks().knownSubjects(), value -> { });
        HTMLElement newRow = Dom.div("new-subject");
        newRow.setHidden(true);
        HTMLInputElement newName = Dom.input("text", "input");
        newName.setPlaceholder("e.g. Biology");
        newName.setAttribute("aria-label", "New subject name");
        newName.setAttribute("maxlength", "40");
        Runnable addSubject = () -> {
            String name = newName.getValue().trim();
            if (!name.isEmpty()) {
                subjects.addValue(name);
                subjects.select(name);
                newName.setValue("");
                newRow.setHidden(true);
            }
        };
        newName.addEventListener("keydown", event -> {
            if ("Enter".equals(((KeyboardEvent) event).getKey())) {
                event.preventDefault();
                addSubject.run();
            }
        });
        Dom.append(newRow, newName, Dom.button("chip on", "Add", addSubject));
        subjects.setExtraButton("+ New", () -> {
            newRow.setHidden(false);
            newName.focus();
        });
        Dom.append(box, subjects.getElement(), newRow);
        return box;
    }

    private void onDueChip(String value) {
        dueField.clearError();
        picker.setHidden(!PICK.equals(value));
        if (PICK.equals(value)) {
            pickDate.focus();
        }
    }

    /** Quick chips mean "by 11:59 PM that day"; Pick a date uses the chosen date and time. */
    private long chosenDueAt() {
        String chip = dueChips.getSelected();
        if (chip == null) {
            return 0;
        }
        AppClock clock = app().services().getClock();
        LocalDay today = clock.today();
        switch (chip) {
            case TODAY:
                return today.atTime(23, 59, clock);
            case TOMORROW:
                return today.plusDays(1).atTime(23, 59, clock);
            case NEXT_WEEK:
                return today.plusDays(7).atTime(23, 59, clock);
            default:
                LocalDay day = LocalDay.parseIso(pickDate.getValue());
                if (day == null) {
                    return 0;
                }
                String time = pickTime.getValue();
                int hour = 23;
                int minute = 59;
                if (time != null && time.length() >= 5) {
                    hour = Integer.parseInt(time.substring(0, 2));
                    minute = Integer.parseInt(time.substring(3, 5));
                }
                return day.atTime(hour, minute, clock);
        }
    }

    private void save() {
        titleField.clearError();
        dueField.clearError();
        formError.setHidden(true);
        String subject = subjects == null ? null : subjects.getSelected();
        submit.setAttribute("disabled", "");
        app().services().getTasks().addTask(title.getValue(), chosenDueAt(), subject, notes.getValue(), task -> {
            app().closeSheet();
            app().getTaskList().highlight(task.getId());
            app().showTasks();
            app().toast().show("Added to your list.");
        }, this::showError);
    }

    private void showError(ValidationError error) {
        submit.removeAttribute("disabled");
        if ("title".equals(error.getField())) {
            titleField.setError(error.getMessage());
            title.focus();
        } else if ("due".equals(error.getField())) {
            dueField.setError(error.getMessage());
        } else {
            Dom.clear(formError);
            formError.appendChild(Dom.text("span", "", error.getMessage() + " "));
            if (error.isPlanLimit()) {
                formError.appendChild(Dom.button("link", "See plans", () -> {
                    app().closeSheet();
                    app().showPlans();
                }));
            }
            formError.setHidden(false);
        }
    }
}
