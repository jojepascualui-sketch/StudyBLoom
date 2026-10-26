package app.studybloom.time;

/** The headings unfinished tasks are grouped under on the home screen. */
public enum TaskGroup {
    OVERDUE("Overdue"),
    TODAY("Today"),
    THIS_WEEK("This week"),
    LATER("Later");

    private final String label;

    TaskGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
