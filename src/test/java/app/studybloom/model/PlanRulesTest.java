package app.studybloom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The plan rules from the brief, checked through the abstract Subscription type. */
class PlanRulesTest {

    @Test
    void freePlanAllowsFifteenUnfinishedTasks() {
        Subscription free = new FreePlan();
        assertEquals(15, free.getMaxActiveTasks());
        assertTrue(free.canAddTask(14));
        assertFalse(free.canAddTask(15));
        assertFalse(free.isUnlimited());
        assertEquals(0, free.getMonthlyPricePesos());
    }

    @Test
    void freePlanHasNoPaidFeatures() {
        Subscription free = new FreePlan();
        assertFalse(free.canUseReminders());
        assertFalse(free.canUseSubjectTags());
        assertFalse(free.canUseSchedule());
        assertFalse(free.canUseNotes());
    }

    @Test
    void proPlanIsUnlimitedWithTagsAndReminders() {
        Subscription pro = new ProPlan();
        assertTrue(pro.isUnlimited());
        assertTrue(pro.canAddTask(10_000));
        assertTrue(pro.canUseReminders());
        assertTrue(pro.canUseSubjectTags());
        assertFalse(pro.canUseSchedule());
        assertFalse(pro.canUseNotes());
        assertEquals(49, pro.getMonthlyPricePesos());
    }

    @Test
    void maxPlanIsEverythingInProPlusScheduleAndNotes() {
        Subscription max = new MaxPlan();
        assertInstanceOf(ProPlan.class, max, "Max inherits from Pro");
        assertTrue(max.isUnlimited());
        assertTrue(max.canUseReminders());
        assertTrue(max.canUseSubjectTags());
        assertTrue(max.canUseSchedule());
        assertTrue(max.canUseNotes());
        assertEquals(99, max.getMonthlyPricePesos());
    }

    @Test
    void factoryReturnsTheRightSubclass() {
        assertInstanceOf(FreePlan.class, Subscription.forPlan(PlanType.FREE));
        assertInstanceOf(ProPlan.class, Subscription.forPlan(PlanType.PRO));
        assertInstanceOf(MaxPlan.class, Subscription.forPlan(PlanType.MAX));
        assertEquals(PlanType.FREE, PlanType.fromCode("something-unknown"));
        assertEquals(PlanType.MAX, PlanType.fromCode("max"));
    }

    @Test
    void featuresMapToTheCheapestPlanThatUnlocksThem() {
        assertEquals(PlanType.PRO, Feature.SUBJECT_TAGS.getRequiredPlan());
        assertEquals(PlanType.PRO, Feature.REMINDERS.getRequiredPlan());
        assertEquals(PlanType.MAX, Feature.SCHEDULE.getRequiredPlan());
        assertEquals(PlanType.MAX, Feature.NOTES.getRequiredPlan());
        for (Feature feature : Feature.values()) {
            for (PlanType type : PlanType.values()) {
                boolean expected = type.isAtLeast(feature.getRequiredPlan());
                assertEquals(expected, feature.isAvailableOn(Subscription.forPlan(type)), feature + " on " + type);
            }
        }
    }
}
