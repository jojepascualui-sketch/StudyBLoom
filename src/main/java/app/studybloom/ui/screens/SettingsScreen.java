package app.studybloom.ui.screens;

import app.studybloom.AppServices;
import app.studybloom.model.Feature;
import app.studybloom.model.PlanType;
import app.studybloom.model.Student;
import app.studybloom.model.Task;
import app.studybloom.service.Preferences;
import app.studybloom.service.TaskService;
import app.studybloom.time.TaskGroup;
import app.studybloom.ui.App;
import app.studybloom.ui.Dom;
import app.studybloom.ui.Icons;
import app.studybloom.ui.NavTab;
import app.studybloom.ui.Screen;
import app.studybloom.ui.components.LockTag;
import app.studybloom.ui.components.TaskCard;
import app.studybloom.ui.components.Toggle;
import app.studybloom.web.Js;
import java.util.List;
import org.teavm.jso.dom.html.HTMLElement;

/** Screen 6: Me. Install card, dark mode, reminders, demo mode, sign out, and what's due today. */
public class SettingsScreen extends Screen implements TaskCard.Listener {
    private Toggle reminderToggle;

    public SettingsScreen(App app) {
        super(app);
    }

    @Override
    public NavTab getTab() {
        return NavTab.ME;
    }

    @Override
    public String getTitle() {
        return "Me";
    }

    @Override
    protected HTMLElement build() {
        AppServices services = app().services();
        Student student = services.getAuth().getCurrentStudent();
        HTMLElement page = Dom.div("me-screen");
        Dom.append(page, Dom.text("h1", "h-title", "Me"),
                Dom.text("p", "h-sub me-email", student == null ? "" : student.getEmail()));

        page.appendChild(buildInstall());

        page.appendChild(Dom.text("h2", "group-label", "Settings"));
        page.appendChild(planRow());

        Toggle dark = new Toggle("Dark mode", app().isDarkNow(), on -> {
            services.getPreferences().setTheme(on ? Preferences.Theme.DARK : Preferences.Theme.LIGHT);
            app().applyTheme();
        });
        page.appendChild(row("Dark mode", dark.getElement()));

        if (services.getSubscriptions().isAvailable(Feature.REMINDERS)) {
            boolean on = services.getPreferences().isRemindersEnabled() && "granted".equals(Js.notificationPermission());
            reminderToggle = new Toggle("Reminders", on, this::setReminders);
            page.appendChild(row("Reminders", reminderToggle.getElement()));
        } else {
            page.appendChild(row("Reminders", LockTag.create(PlanType.PRO, app()::showPlans)));
        }

        if (!services.isLocalMode()) {
            Toggle demo = new Toggle("Demo mode", services.getPreferences().isDemoMode(), on -> {
                services.getPreferences().setDemoMode(on);
                app().toast().show(on ? "Demo mode on. Upgrades in Plans are free and instant."
                        : "Demo mode off. Upgrades go through Stripe test checkout.");
            });
            page.appendChild(row("Demo mode", demo.getElement()));
        }

        HTMLElement signOut = Dom.button("row row-button sign-out", "Sign out", app()::signOut);
        page.appendChild(signOut);

        List<Task> today = services.getTasks().groupUnfinished().get(TaskGroup.TODAY);
        if (today != null && !today.isEmpty()) {
            page.appendChild(Dom.text("h2", "group-label", "Due today"));
            TaskService tasks = services.getTasks();
            for (Task task : today) {
                page.appendChild(new TaskCard(task, tasks.describeDue(task), this).render());
            }
        }

        if (services.isLocalMode()) {
            page.appendChild(Dom.text("p", "fine-print",
                    "Local demo mode: Supabase isn't configured, so data stays in this browser."));
        }
        return page;
    }

    /** Install card: Android/desktop use beforeinstallprompt; iPhone gets instructions instead. */
    private HTMLElement buildInstall() {
        HTMLElement box = Dom.div("install-box");
        if (Js.isStandalone()) {
            return box;
        }
        boolean canPrompt = Js.hasInstallPrompt();
        boolean ios = Js.isIos();
        if (!canPrompt && !ios) {
            return box;
        }
        HTMLElement card = Dom.div("install");
        HTMLElement text = Dom.div("install-text");
        Dom.append(text, Dom.text("div", "install-title", "Install StudyBloom"),
                Dom.text("div", "h-sub small", ios && !canPrompt
                        ? "Tap Share, then Add to Home Screen."
                        : "Open it from your home screen, even offline."));
        Dom.append(card, Dom.svg("div", "app-icon", Icons.appIcon()), text);
        box.appendChild(card);
        if (canPrompt) {
            box.appendChild(Dom.button("btn btn-primary install-btn", "Add to home screen", () -> Js.promptInstall(outcome -> {
                if ("accepted".equals(outcome)) {
                    app().toast().show("Installed! Find StudyBloom on your home screen.");
                }
                app().rerender();
            })));
        }
        return box;
    }

    private HTMLElement planRow() {
        String name = app().services().getSubscriptions().getCurrent().getDisplayName();
        HTMLElement button = Dom.button("row row-button", "", app()::showPlans);
        Dom.append(button, Dom.text("span", "row-label", "Plan"), Dom.text("span", "row-value", name + " ›"));
        button.setAttribute("aria-label", "Plan: " + name + ". See plans");
        return button;
    }

    private static HTMLElement row(String label, HTMLElement control) {
        HTMLElement row = Dom.div("row");
        Dom.append(row, Dom.text("span", "row-label", label), control);
        return row;
    }

    private void setReminders(boolean on) {
        AppServices services = app().services();
        if (!on) {
            services.getPreferences().setRemindersEnabled(false);
            return;
        }
        Js.requestNotificationPermission(permission -> {
            if ("granted".equals(permission)) {
                services.getPreferences().setRemindersEnabled(true);
                services.getReminders().check(services.getTasks().getUnfinished());
                app().toast().show("Reminders on. We'll nudge you a day and an hour before deadlines.");
            } else {
                services.getPreferences().setRemindersEnabled(false);
                reminderToggle.setOn(false);
                app().toast().show("Notifications are blocked. Allow them in your browser's site settings.");
            }
        });
    }

    @Override
    public void onToggle(TaskCard card) {
        TaskService tasks = app().services().getTasks();
        Task task = card.getTask();
        app().toast().flush();
        tasks.complete(task, () -> { }, error -> app().toast().show(error));
        app().rerender();
        app().toast().show("Marked complete. Nice one!", "Undo", () -> {
            tasks.undoComplete(task, () -> { }, error -> app().toast().show(error));
            app().rerender();
        }, 5000, null);
    }

    @Override
    public void onOpen(TaskCard card) {
        app().showTasks();
    }
}
