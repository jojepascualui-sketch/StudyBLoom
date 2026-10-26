package app.studybloom.model;

import java.util.List;

/** INHERITANCE + POLYMORPHISM: overrides the Free answers with Pro ones. */
public class ProPlan extends Subscription {
    @Override
    public PlanType getPlanType() {
        return PlanType.PRO;
    }

    @Override
    public String getDisplayName() {
        return "Pro";
    }

    @Override
    public int getMonthlyPricePesos() {
        return 49;
    }

    @Override
    public int getMaxActiveTasks() {
        return UNLIMITED;
    }

    @Override
    public boolean canUseReminders() {
        return true;
    }

    @Override
    public boolean canUseSubjectTags() {
        return true;
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
        return List.of("Unlimited tasks", "Subject tags and colors", "Reminders before deadlines");
    }
}
