package app.studybloom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.studybloom.fakes.FakeSubscriptionRepository;
import app.studybloom.fakes.FixedClock;
import app.studybloom.fakes.MapStore;
import app.studybloom.model.PlanType;
import app.studybloom.model.Task;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReminderServiceTest {
    private final List<String> sent = new ArrayList<>();
    private FixedClock clock;
    private FakeSubscriptionRepository plans;
    private SubscriptionService subscriptions;
    private Preferences preferences;
    private ReminderService reminders;

    @BeforeEach
    void setUp() {
        clock = new FixedClock();
        MapStore store = new MapStore();
        plans = new FakeSubscriptionRepository();
        subscriptions = new SubscriptionService(plans);
        preferences = new Preferences(store);
        preferences.setRemindersEnabled(true);
        Notifier notifier = new Notifier() {
            @Override
            public boolean isPermitted() {
                return true;
            }

            @Override
            public void notify(String title, String body, String tag) {
                sent.add(title + " | " + body);
            }
        };
        reminders = new ReminderService(clock, store, notifier, subscriptions, preferences);
    }

    private void usePro() {
        plans.setPlan(PlanType.PRO);
        subscriptions.load("bea", () -> { }, e -> { });
    }

    @Test
    void freePlanGetsNoReminders() {
        List<Task> tasks = List.of(new Task("a", "bea", "Essay", clock.at(0, 10, 30)));
        assertEquals(0, reminders.check(tasks));
    }

    @Test
    void proSendsOneDayAndOneHourRemindersOnce() {
        usePro();
        List<Task> tasks = List.of(
                new Task("a", "bea", "Essay", clock.at(0, 20, 0)),     // 10 h away -> 24h reminder
                new Task("b", "bea", "Quiz", clock.at(0, 10, 45)),     // 45 min away -> 1h reminder
                new Task("c", "bea", "Project", clock.at(5, 9, 0)));   // too far
        assertEquals(2, reminders.check(tasks));
        assertEquals(0, reminders.check(tasks), "never twice");

        clock.advanceMinutes(9 * 60 + 10); // now 19:10, essay is 50 min away
        assertEquals(1, reminders.check(tasks));
        assertEquals(3, sent.size());
    }

    @Test
    void turnedOffMeansNothing() {
        usePro();
        preferences.setRemindersEnabled(false);
        assertEquals(0, reminders.check(List.of(new Task("a", "bea", "Essay", clock.at(0, 10, 30)))));
    }
}
