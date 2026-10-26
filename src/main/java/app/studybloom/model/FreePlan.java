package app.studybloom.model;

import java.util.List;

/** INHERITANCE: a Subscription with the Free limits. */
public class FreePlan extends Subscription {
    public static final int FREE_TASK_LIMIT = 15;

    @Override
    public PlanType getPlanType() {
        return PlanType.FREE;
    }

    @Override
    public String getDisplayName() {
        return "Free";
    }

    @Override
    public int getMonthlyPricePesos() {
        return 0;
    }

    @Override
    public int getMaxActiveTasks() {
        return FREE_TASK_LIMIT;
    }

    @Override
    public boolean canUseReminders() {
        return false;
    }

    @Override
    public boolean canUseSubjectTags() {
        return false;
    }

    @Override
    public boolean canUseSchedule() {
        return false;
    }

    @Override
    public boolean canUseNotes() {
        return false;
    }

    @Override
    public List<String> getFeatures() {
        return List.of("Up to 15 unfinished tasks", "Due dates and overdue alerts");
    }
}
