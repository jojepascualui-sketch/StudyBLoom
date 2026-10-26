package app.studybloom.model;

/** Paid features, each with the cheapest plan that unlocks it. */
public enum Feature {
    SUBJECT_TAGS(PlanType.PRO),
    REMINDERS(PlanType.PRO),
    SCHEDULE(PlanType.MAX),
    NOTES(PlanType.MAX);

    private final PlanType requiredPlan;

    Feature(PlanType requiredPlan) {
        this.requiredPlan = requiredPlan;
    }

    public PlanType getRequiredPlan() {
        return requiredPlan;
    }

    /** POLYMORPHISM: the answer comes from whichever Subscription subclass is passed in. */
    public boolean isAvailableOn(Subscription plan) {
        switch (this) {
            case SUBJECT_TAGS:
                return plan.canUseSubjectTags();
            case REMINDERS:
                return plan.canUseReminders();
            case SCHEDULE:
                return plan.canUseSchedule();
            default:
                return plan.canUseNotes();
        }
    }
}
