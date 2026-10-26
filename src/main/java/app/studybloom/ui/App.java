package app.studybloom.ui;

import app.studybloom.AppServices;
import app.studybloom.model.Student;
import app.studybloom.service.Preferences;
import app.studybloom.ui.components.BottomSheet;
import app.studybloom.ui.components.NavBar;
import app.studybloom.ui.components.Toast;
import app.studybloom.ui.screens.LoginScreen;
import app.studybloom.ui.screens.PricingScreen;
import app.studybloom.ui.screens.ScheduleScreen;
import app.studybloom.ui.screens.SettingsScreen;
import app.studybloom.ui.screens.TaskListScreen;
import app.studybloom.web.Js;
import app.studybloom.web.OfflineTaskRepository;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * The shell: owns the layout (screen area, bottom nav, toast, sheets) and
 * switches between screens. POLYMORPHISM: show(Screen) accepts any subclass.
 */
public class App {
    private final AppServices services;
    private final HTMLElement root;
    private HTMLElement screenHost;
    private HTMLElement overlayHost;
    private HTMLElement offlineBanner;
    private NavBar nav;
    private Toast toast;
    private Screen current;
    private TaskListScreen taskList;
    private BottomSheet sheet;

    public App(AppServices services, HTMLElement root) {
        this.services = services;
        this.root = root;
    }

    public void start() {
        Dom.clear(root);
        offlineBanner = Dom.text("div", "offline-banner",
                "You're offline. Showing saved tasks; changes will sync when you reconnect.");
        offlineBanner.setAttribute("role", "status");
        offlineBanner.setHidden(Js.isOnline());
        screenHost = Dom.el("main", "screen");
        screenHost.setAttribute("id", "main");
        overlayHost = Dom.div("overlays");
        nav = new NavBar(this::navigate);
        toast = new Toast();
        Dom.append(root, offlineBanner, screenHost, toast.getElement(), nav.getElement(), overlayHost);

        applyTheme();
        Js.onSystemThemeChange(this::applyTheme);
        Js.onWindowEvent("offline", () -> offlineBanner.setHidden(false));
        Js.onWindowEvent("online", this::onBackOnline);
        Js.onVisible(this::quietRefresh);
        Js.onInstallStateChange(() -> {
            if (current instanceof SettingsScreen) {
                rerender();
            }
        });
        if (services.getSupabase() != null) {
            services.getSupabase().setOnSessionExpired(() -> {
                services.getAuth().signOut(() -> { });
                show(new LoginScreen(this));
                toast.show("Your session ended. Please sign in again.");
            });
        }
        services.getSubscriptions().addChangeListener(() -> {
            if (current != null && current.getTab() != null) {
                rerender();
            }
        });
        Window.setInterval(this::checkReminders, 60_000);

        if (services.getAuth().restore()) {
            enterApp();
        } else {
            show(new LoginScreen(this));
        }
    }

    /** After sign-in (or a restored session): load plan and tasks, then show My tasks. */
    public void enterApp() {
        Student student = services.getAuth().getCurrentStudent();
        services.getTasks().setUserId(student.getId());
        taskList = new TaskListScreen(this);
        taskList.setLoading(true);
        show(taskList);
        services.getSubscriptions().load(student.getId(), this::loadTasks, error -> loadTasks());
        handleCheckoutReturn(student);
    }

    private void loadTasks() {
        services.getTasks().refresh(() -> {
            taskList.setLoading(false);
            if (current == taskList) {
                rerender();
            }
            checkReminders();
        }, error -> {
            taskList.setLoading(false);
            rerender();
            toast.show(error);
        });
    }

    private void handleCheckoutReturn(Student student) {
        String checkout = Js.queryParam("checkout");
        if (checkout.isEmpty()) {
            return;
        }
        Js.clearQuery();
        if ("success".equals(checkout)) {
            toast.show("Payment received. Your plan updates in a few seconds.");
            // The webhook usually lands within a second or two; check twice.
            Window.setTimeout(() -> services.getSubscriptions().load(student.getId(), () -> { }, e -> { }), 3000);
            Window.setTimeout(() -> services.getSubscriptions().load(student.getId(), () -> { }, e -> { }), 9000);
        } else {
            toast.show("Checkout canceled. You're still on " + services.getSubscriptions().getCurrent().getDisplayName() + ".");
        }
    }

    // ---- navigation ----------------------------------------------------

    public void show(Screen screen) {
        closeSheet();
        current = screen;
        Dom.clear(screenHost);
        screenHost.appendChild(screen.render());
        nav.setActive(screen.getTab());
        Dom.toggleClass(root, "with-nav", screen.getTab() != null);
        Dom.doc().setTitle(screen.getTitle() + " · StudyBloom");
        Window.current().scrollTo(0, 0);
        screen.onShown();
    }

    /** Re-renders the current screen in place (scroll position kept). */
    public void rerender() {
        if (current == null) {
            return;
        }
        int scroll = Window.current().getScrollY();
        Dom.clear(screenHost);
        screenHost.appendChild(current.render());
        Window.current().scrollTo(0, scroll);
    }

    public void navigate(NavTab tab) {
        switch (tab) {
            case TASKS:
                showTasks();
                break;
            case SCHEDULE:
                show(new ScheduleScreen(this));
                break;
            case PLANS:
                showPlans();
                break;
            default:
                show(new SettingsScreen(this));
        }
    }

    public void showTasks() {
        show(taskList);
    }

    public void showPlans() {
        show(new PricingScreen(this));
    }

    public TaskListScreen getTaskList() {
        return taskList;
    }

    public Screen getCurrent() {
        return current;
    }

    // ---- overlays ------------------------------------------------------

    public void openSheet(HTMLElement content, String label) {
        closeSheet();
        sheet = new BottomSheet(content, label, () -> sheet = null);
        sheet.open(overlayHost);
    }

    public void closeSheet() {
        if (sheet != null) {
            BottomSheet open = sheet;
            sheet = null;
            open.close();
        }
    }

    public Toast toast() {
        return toast;
    }

    // ---- app-wide actions ----------------------------------------------

    public AppServices services() {
        return services;
    }

    public void applyTheme() {
        Preferences.Theme theme = services.getPreferences().getTheme();
        Js.setDarkClass(isDark(theme));
    }

    public boolean isDarkNow() {
        return isDark(services.getPreferences().getTheme());
    }

    private static boolean isDark(Preferences.Theme theme) {
        return theme == Preferences.Theme.DARK || (theme == Preferences.Theme.SYSTEM && Js.prefersDark());
    }

    public void signOut() {
        toast.flush();
        OfflineTaskRepository offline = services.getOfflineTasks();
        if (offline != null && offline.pendingCount() > 0
                && !Window.confirm("Some changes haven't synced yet. Sign out anyway and lose them?")) {
            return;
        }
        services.getAuth().signOut(() -> {
            if (offline != null) {
                offline.clear();
            }
            show(new LoginScreen(this));
        });
    }

    private void onBackOnline() {
        offlineBanner.setHidden(true);
        OfflineTaskRepository offline = services.getOfflineTasks();
        if (offline == null || !services.getAuth().isSignedIn()) {
            return;
        }
        boolean hadChanges = offline.pendingCount() > 0;
        services.getTasks().refresh(() -> {
            String problem = offline.takeLastSyncProblem();
            if (problem != null) {
                toast.show("One change couldn't sync: " + problem);
            } else if (hadChanges) {
                toast.show("Back online. Your changes are synced.");
            }
            if (current == taskList) {
                rerender();
            }
        }, error -> { });
    }

    private void quietRefresh() {
        if (!services.getAuth().isSignedIn() || taskList == null || sheet != null) {
            return;
        }
        services.getTasks().refresh(() -> {
            if (current == taskList) {
                rerender();
            }
            checkReminders();
        }, error -> { });
    }

    private void checkReminders() {
        if (services.getAuth().isSignedIn()) {
            services.getReminders().check(services.getTasks().getUnfinished());
        }
    }
}
