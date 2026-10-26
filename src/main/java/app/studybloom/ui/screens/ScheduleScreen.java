package app.studybloom.ui.screens;

import app.studybloom.AppServices;
import app.studybloom.model.Feature;
import app.studybloom.model.PlanType;
import app.studybloom.model.ScheduleEntry;
import app.studybloom.model.Student;
import app.studybloom.model.SubjectNote;
import app.studybloom.time.DateTexts;
import app.studybloom.ui.App;
import app.studybloom.ui.Dom;
import app.studybloom.ui.NavTab;
import app.studybloom.ui.Screen;
import app.studybloom.ui.components.ChipGroup;
import app.studybloom.ui.components.FormField;
import app.studybloom.ui.components.LockTag;
import app.studybloom.ui.components.SubjectColors;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLTextAreaElement;

/**
 * Extension (the Schedule tab has no mockup): Max plan class schedule and
 * notes per subject, drawn in the same style as the other screens.
 */
public class ScheduleScreen extends Screen {
    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private final List<ScheduleEntry> entries = new ArrayList<>();
    private final List<SubjectNote> notes = new ArrayList<>();
    private boolean loaded;
    private boolean formOpen;

    public ScheduleScreen(App app) {
        super(app);
    }

    @Override
    public NavTab getTab() {
        return NavTab.SCHEDULE;
    }

    @Override
    public String getTitle() {
        return "Schedule";
    }

    @Override
    protected HTMLElement build() {
        HTMLElement page = Dom.div("schedule-screen");
        Dom.append(page, Dom.text("h1", "h-title", "Schedule"),
                Dom.text("p", "h-sub", "Your classes and notes for each subject."));

        if (!app().services().getSubscriptions().isAvailable(Feature.SCHEDULE)) {
            page.appendChild(lockedCard());
            return page;
        }
        if (!loaded) {
            page.appendChild(Dom.text("p", "empty", "Loading your schedule…"));
            return page;
        }

        page.appendChild(Dom.text("h2", "group-label", "Classes"));
        if (entries.isEmpty()) {
            page.appendChild(Dom.text("p", "empty", "No classes yet. Add your first one below."));
        }
        for (int day = 1; day <= 7; day++) {
            for (ScheduleEntry entry : entries) {
                if (entry.getDayOfWeek() == day) {
                    page.appendChild(classCard(entry));
                }
            }
        }
        page.appendChild(formOpen ? addForm() : Dom.button("btn btn-ghost", "+ Add a class", () -> {
            formOpen = true;
            app().rerender();
        }));

        page.appendChild(Dom.text("h2", "group-label", "Notes"));
        List<String> subjects = allSubjects();
        if (subjects.isEmpty()) {
            page.appendChild(Dom.text("p", "empty", "Add a class or a task with a subject to start taking notes."));
        }
        for (String subject : subjects) {
            page.appendChild(noteCard(subject));
        }
        return page;
    }

    @Override
    public void onShown() {
        if (!app().services().getSubscriptions().isAvailable(Feature.SCHEDULE) || loaded) {
            return;
        }
        AppServices services = app().services();
        String userId = services.getAuth().getCurrentStudent().getId();
        services.getSchedules().findAll(userId, list -> {
            entries.clear();
            entries.addAll(list);
            entries.sort((a, b) -> a.getDayOfWeek() != b.getDayOfWeek()
                    ? a.getDayOfWeek() - b.getDayOfWeek() : a.getStartTime().compareTo(b.getStartTime()));
            services.getNotes().findAll(userId, noteList -> {
                notes.clear();
                notes.addAll(noteList);
                loaded = true;
                rerenderIfShown();
            }, error -> showLoadError(error.getMessage()));
        }, error -> showLoadError(error.getMessage()));
    }

    private void showLoadError(String message) {
        loaded = true;
        rerenderIfShown();
        app().toast().show(message);
    }

    private void rerenderIfShown() {
        if (app().getCurrent() == this) {
            app().rerender();
        }
    }

    private HTMLElement lockedCard() {
        HTMLElement card = Dom.div("plan locked-card");
        HTMLElement top = Dom.div("plan-top");
        Dom.append(top, Dom.text("h2", "plan-name max-name", "Class schedule"), LockTag.create(PlanType.MAX, app()::showPlans));
        HTMLElement list = Dom.el("ul", "");
        Dom.append(list, Dom.text("li", "", "Your weekly classes, times and rooms"),
                Dom.text("li", "", "Notes for each subject, always with you"));
        Dom.append(card, top, list, Dom.button("btn btn-ghost btn-max", "See Max", app()::showPlans));
        return card;
    }

    private HTMLElement classCard(ScheduleEntry entry) {
        HTMLElement card = Dom.div("task class-card");
        HTMLElement body = Dom.div("task-body");
        String when = DateTexts.weekday(entry.getDayOfWeek()) + ", " + DateTexts.time(entry.getStartTime())
                + " – " + DateTexts.time(entry.getEndTime());
        HTMLElement meta = Dom.div("meta");
        meta.appendChild(Dom.text("span", SubjectColors.tagClass(entry.getSubject()), entry.getSubject()));
        if (entry.getRoom() != null) {
            meta.appendChild(Dom.text("span", "due", entry.getRoom()));
        }
        Dom.append(body, Dom.text("div", "t", when), meta);
        HTMLElement remove = Dom.button("icon-btn", "×", () -> app().services().getSchedules().delete(entry.getId(), () -> {
            entries.remove(entry);
            app().rerender();
        }, error -> app().toast().show(error.getMessage())));
        remove.setAttribute("aria-label", "Remove " + entry.getSubject() + " on " + DateTexts.weekday(entry.getDayOfWeek()));
        Dom.append(card, body, remove);
        return card;
    }

    private HTMLElement addForm() {
        HTMLElement form = Dom.el("form", "plan add-class");
        form.setAttribute("novalidate", "");
        HTMLInputElement subject = Dom.input("text", "input");
        subject.setPlaceholder("e.g. Calculus 1");
        subject.setAttribute("maxlength", "40");
        FormField subjectField = new FormField("Subject", subject);

        List<String> dayNames = new ArrayList<>();
        for (String d : DAYS) {
            dayNames.add(d);
        }
        ChipGroup days = new ChipGroup("Day", dayNames, value -> { });
        days.select("Mon");
        HTMLElement dayBox = Dom.div("");
        dayBox.appendChild(days.getElement());
        FormField dayField = new FormField("Day", dayBox);

        HTMLInputElement start = Dom.input("time", "input");
        start.setValue("08:00");
        HTMLInputElement end = Dom.input("time", "input");
        end.setValue("09:30");
        HTMLElement times = Dom.div("date-picker");
        Dom.append(times, new FormField("Starts", start).getElement(), new FormField("Ends", end).getElement());

        HTMLInputElement room = Dom.input("text", "input");
        room.setPlaceholder("Room 204 (optional)");
        FormField roomField = new FormField("Room", room);

        HTMLElement save = Dom.el("button", "btn btn-primary");
        save.setAttribute("type", "submit");
        save.setTextContent("Save class");
        form.addEventListener("submit", event -> {
            event.preventDefault();
            subjectField.clearError();
            if (subject.getValue().trim().isEmpty()) {
                subjectField.setError("Name the subject, like “Gen Chem”.");
                return;
            }
            if (end.getValue().compareTo(start.getValue()) <= 0) {
                subjectField.setError("The class has to end after it starts. Check the times.");
                return;
            }
            Student student = app().services().getAuth().getCurrentStudent();
            ScheduleEntry entry = new ScheduleEntry();
            entry.setId(app().services().getIds().newId());
            entry.setUserId(student.getId());
            entry.setSubject(subject.getValue());
            entry.setDayOfWeek(dayIndex(days.getSelected()));
            entry.setStartTime(start.getValue());
            entry.setEndTime(end.getValue());
            entry.setRoom(room.getValue());
            app().services().getSchedules().insert(entry, saved -> {
                entries.add(saved);
                entries.sort((a, b) -> a.getDayOfWeek() != b.getDayOfWeek()
                        ? a.getDayOfWeek() - b.getDayOfWeek() : a.getStartTime().compareTo(b.getStartTime()));
                formOpen = false;
                app().rerender();
                app().toast().show("Class added.");
            }, error -> subjectField.setError(error.getMessage()));
        });
        Dom.append(form, subjectField.getElement(), dayField.getElement(), times, roomField.getElement(), save,
                Dom.div("spacer-10"), Dom.button("btn btn-ghost", "Cancel", () -> {
                    formOpen = false;
                    app().rerender();
                }));
        return form;
    }

    private HTMLElement noteCard(String subject) {
        SubjectNote existing = null;
        for (SubjectNote note : notes) {
            if (note.getSubject().equalsIgnoreCase(subject)) {
                existing = note;
            }
        }
        final SubjectNote current = existing;
        HTMLElement card = Dom.div("plan note-card");
        HTMLTextAreaElement text = Dom.textarea("input area");
        text.setValue(current == null ? "" : current.getBody());
        text.setPlaceholder("Key formulas, reminders, what the teacher stressed…");
        FormField field = new FormField(subject, text);
        HTMLElement save = Dom.button("btn btn-ghost btn-small", "Save note", () -> saveNote(subject, current, text.getValue()));
        Dom.append(card, field.getElement(), save);
        return card;
    }

    private void saveNote(String subject, SubjectNote existing, String body) {
        AppServices services = app().services();
        long now = services.getClock().nowMillis();
        if (existing == null) {
            SubjectNote note = new SubjectNote();
            note.setId(services.getIds().newId());
            note.setUserId(services.getAuth().getCurrentStudent().getId());
            note.setSubject(subject);
            note.setBody(body);
            note.setUpdatedAt(now);
            services.getNotes().insert(note, saved -> {
                notes.add(saved);
                app().toast().show("Note saved.");
            }, error -> app().toast().show(error.getMessage()));
        } else {
            existing.setBody(body);
            existing.setUpdatedAt(now);
            services.getNotes().update(existing, saved -> app().toast().show("Note saved."),
                    error -> app().toast().show(error.getMessage()));
        }
    }

    private List<String> allSubjects() {
        Set<String> subjects = new LinkedHashSet<>();
        for (ScheduleEntry entry : entries) {
            subjects.add(entry.getSubject());
        }
        subjects.addAll(app().services().getTasks().knownSubjects());
        for (SubjectNote note : notes) {
            subjects.add(note.getSubject());
        }
        return new ArrayList<>(subjects);
    }

    private static int dayIndex(String name) {
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(name)) {
                return i + 1;
            }
        }
        return 1;
    }
}
