package app.studybloom.model;

import java.util.List;

/**
 * INHERITANCE: Max is "everything in Pro, plus more", so it extends ProPlan and
 * only overrides what is different. Unlimited tasks, tags and reminders are
 * inherited unchanged.
 */
public class MaxPlan extends ProPlan {
    @Override
    public PlanType getPlanType() {
        return PlanType.MAX;
    }

    @Override
    public String getDisplayName() {
        return "Max";
    }

    @Override
    public int getMonthlyPricePesos() {
        return 99;
    }

    @Override
    public boolean canUseSchedule() {
        return true;
    }

    @Override
    public boolean canUseNotes() {
        return true;
    }

    @Override
    public List<String> getFeatures() {
        return List.of("Everything in Pro", "Class schedule", "Notes for each subject");
    }
}
