package app.studybloom.service;

import app.studybloom.model.Feature;
import app.studybloom.model.Task;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.time.AppClock;
import app.studybloom.time.DateTexts;
import java.util.List;

/**
 * Pro feature: notifies the student 24 hours and 1 hour before a deadline.
 * Each reminder is sent once; sent keys are remembered in the store.
 */
public class ReminderService {
    private static final String SENT_KEY = "sb.reminders.sent";
    private static final long HOUR = 3_600_000L;

    private final AppClock clock;
    private final KeyValueStore store;
    private final Notifier notifier;
    private final SubscriptionService subscriptions;
    private final Preferences preferences;

    public ReminderService(AppClock clock, KeyValueStore store, Notifier notifier,
                           SubscriptionService subscriptions, Preferences preferences) {
        this.clock = clock;
        this.store = store;
        this.notifier = notifier;
        this.subscriptions = subscriptions;
        this.preferences = preferences;
    }

    public boolean isActive() {
        return subscriptions.isAvailable(Feature.REMINDERS)
                && preferences.isRemindersEnabled()
                && notifier.isPermitted();
    }

    /** Sends any reminders that are due now. Returns how many were sent. */
    public int check(List<Task> unfinished) {
        if (!isActive()) {
            return 0;
        }
        long now = clock.nowMillis();
        String sent = store.get(SENT_KEY);
        StringBuilder sentKeys = new StringBuilder(sent == null ? "" : sent);
        int count = 0;
        for (Task task : unfinished) {
            long left = task.getDueAt() - now;
            if (task.isCompleted() || left <= 0) {
                continue;
            }
            String window = left <= HOUR ? "1h" : left <= 24 * HOUR ? "24h" : null;
            if (window == null) {
                continue;
            }
            String key = window + ":" + task.getId() + ":" + task.getDueAt() + ";";
            if (sentKeys.indexOf(key) >= 0) {
                continue;
            }
            String body = "1h".equals(window)
                    ? "Due in under an hour, at " + DateTexts.time(task.getDueAt(), clock) + "."
                    : "Due at " + DateTexts.time(task.getDueAt(), clock) + ", less than a day from now.";
            notifier.notify(task.getTitle(), body, key);
            sentKeys.append(key);
            count++;
        }
        if (count > 0) {
            store.set(SENT_KEY, trim(sentKeys.toString()));
        }
        return count;
    }

    /** Keeps the remembered list from growing forever. */
    private static String trim(String keys) {
        int max = 8000;
        if (keys.length() <= max) {
            return keys;
        }
        String tail = keys.substring(keys.length() - max);
        int cut = tail.indexOf(';');
        return cut >= 0 ? tail.substring(cut + 1) : tail;
    }
}
