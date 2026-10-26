package app.studybloom.model;

import java.util.List;

/**
 * A student's plan.
 *
 * ABSTRACTION: callers ask "can this plan use reminders?" without knowing which
 * plan it is. Each subclass answers for itself.
 * POLYMORPHISM: {@link FreePlan}, {@link ProPlan} and {@link MaxPlan} override
 * the methods below, and the rest of the app only holds a {@code Subscription}.
 */
public abstract class Subscription {
    /** Returned by {@link #getMaxActiveTasks()} when there is no limit. */
    public static final int UNLIMITED = Integer.MAX_VALUE;

    private String status = "active";
    private long currentPeriodEnd;
    private boolean demo;

    public abstract PlanType getPlanType();

    public abstract String getDisplayName();

    public abstract int getMonthlyPricePesos();

    public abstract int getMaxActiveTasks();

    public abstract boolean canUseReminders();

    public abstract boolean canUseSubjectTags();

    public abstract boolean canUseSchedule();

    public abstract boolean canUseNotes();

    /** Bullet points for the plan card. */
    public abstract List<String> getFeatures();

    public boolean isUnlimited() {
        return getMaxActiveTasks() == UNLIMITED;
    }

    /** Whether one more unfinished task fits in this plan. */
    public boolean canAddTask(int unfinishedCount) {
        return isUnlimited() || unfinishedCount < getMaxActiveTasks();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(long currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    /** Factory: turns a stored plan code into the right subclass. */
    public static Subscription forPlan(PlanType type) {
        switch (type) {
            case PRO:
                return new ProPlan();
            case MAX:
                return new MaxPlan();
            default:
                return new FreePlan();
        }
    }
}
