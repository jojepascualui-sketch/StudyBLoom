package app.studybloom.ui;

/**
 * ABSTRACTION: a full screen of the app. INHERITANCE: LoginScreen,
 * TaskListScreen, AddTaskScreen, PricingScreen, SettingsScreen and
 * ScheduleScreen all extend it.
 * POLYMORPHISM: App.show(Screen) works with any of them; each one overrides
 * build(), getTab() and getTitle() in its own way.
 */
public abstract class Screen extends Component {
    private final App app;

    protected Screen(App app) {
        this.app = app;
    }

    protected App app() {
        return app;
    }

    /** Which bottom tab is highlighted, or null to hide the bar (sign in). */
    public abstract NavTab getTab();

    /** Used for the browser tab title. */
    public abstract String getTitle();

    /** Called after the screen is on the page, e.g. to move focus. */
    public void onShown() {
    }
}
