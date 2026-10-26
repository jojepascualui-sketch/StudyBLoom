package app.studybloom.service;

import app.studybloom.repo.KeyValueStore;

/** Settings kept on the device: theme, demo mode, reminders. */
public class Preferences {
    public enum Theme { SYSTEM, LIGHT, DARK }

    private static final String THEME = "sb.theme";
    private static final String DEMO = "sb.demoMode";
    private static final String REMINDERS = "sb.reminders";

    private final KeyValueStore store;

    public Preferences(KeyValueStore store) {
        this.store = store;
    }

    public Theme getTheme() {
        String value = store.get(THEME);
        if ("dark".equals(value)) {
            return Theme.DARK;
        }
        if ("light".equals(value)) {
            return Theme.LIGHT;
        }
        return Theme.SYSTEM;
    }

    public void setTheme(Theme theme) {
        if (theme == Theme.SYSTEM) {
            store.remove(THEME);
        } else {
            store.set(THEME, theme == Theme.DARK ? "dark" : "light");
        }
    }

    public boolean isDemoMode() {
        return store.getBoolean(DEMO, false);
    }

    public void setDemoMode(boolean on) {
        store.setBoolean(DEMO, on);
    }

    public boolean isRemindersEnabled() {
        return store.getBoolean(REMINDERS, false);
    }

    public void setRemindersEnabled(boolean on) {
        store.setBoolean(REMINDERS, on);
    }
}
