package app.studybloom.service;

import app.studybloom.model.Task;
import app.studybloom.time.AppClock;
import app.studybloom.time.LocalDay;
import java.util.List;

/**
 * Counts this week's progress for the bloom meter.
 *
 * "This week's tasks" = tasks completed this week (Monday to Sunday) plus
 * unfinished tasks due by Sunday night, including overdue ones. Each petal is
 * 20% of that total.
 */
public class BloomMeterService {
    private static final String[] NUMBER_WORDS = {"Zero", "One", "Two", "Three", "Four", "Five",
            "Six", "Seven", "Eight", "Nine", "Ten"};

    private final AppClock clock;

    public BloomMeterService(AppClock clock) {
        this.clock = clock;
    }

    public BloomProgress compute(List<Task> tasks) {
        LocalDay today = clock.today();
        long weekStart = today.startOfWeek().startMillis(clock);
        long weekEnd = today.endOfWeek().plusDays(1).startMillis(clock);

        int done = 0;
        int open = 0;
        for (Task task : tasks) {
            if (task.isCompleted()) {
                if (task.getCompletedAt() >= weekStart && task.getCompletedAt() < weekEnd) {
                    done++;
                }
            } else if (task.getDueAt() < weekEnd) {
                open++;
            }
        }
        return build(done, done + open);
    }

    /** Petals = floor(done / total * 5). Visible for tests. */
    public static int petalsFor(int done, int total) {
        if (total <= 0 || done <= 0) {
            return 0;
        }
        if (done >= total) {
            return BloomProgress.PETALS;
        }
        return done * BloomProgress.PETALS / total;
    }

    private BloomProgress build(int done, int total) {
        int petals = petalsFor(done, total);
        if (total == 0) {
            return new BloomProgress(0, 0, 0, "Nothing due this week",
                    "Add an assignment and watch your flower grow.");
        }
        String headline = done + " of " + total + " done this week";
        int left = total - done;
        String message;
        if (left == 0) {
            message = "Your flower is in full bloom. Great week!";
        } else if (done == 0) {
            message = "Finish one to grow your first petal.";
        } else if (left == 1) {
            message = "One more petal to go.";
        } else {
            message = words(left) + " more and your flower is in full bloom.";
        }
        return new BloomProgress(done, total, petals, headline, message);
    }

    private static String words(int n) {
        return n < NUMBER_WORDS.length ? NUMBER_WORDS[n] : String.valueOf(n);
    }
}
