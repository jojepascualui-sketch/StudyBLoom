package app.studybloom.ui;

/** The four bottom-bar tabs from the mockups. */
public enum NavTab {
    TASKS("Tasks", Icons.TASKS),
    SCHEDULE("Schedule", Icons.SCHEDULE),
    PLANS("Plans", Icons.PLANS),
    ME("Me", Icons.ME);

    private final String label;
    private final String icon;

    NavTab(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }
}
