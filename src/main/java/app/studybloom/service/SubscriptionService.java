package app.studybloom.service;

import app.studybloom.model.Feature;
import app.studybloom.model.FreePlan;
import app.studybloom.model.MaxPlan;
import app.studybloom.model.PlanType;
import app.studybloom.model.ProPlan;
import app.studybloom.model.Student;
import app.studybloom.model.Subscription;
import app.studybloom.repo.SubscriptionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Knows the student's current plan and answers "is this allowed?". This is the
 * Java half of plan enforcement; the database trigger and RLS policies are the
 * other half, so the rules hold even if someone edits the JavaScript.
 */
public class SubscriptionService {
    private final SubscriptionRepository repository;
    private final List<Runnable> listeners = new ArrayList<>();
    private Subscription current = new FreePlan();

    public SubscriptionService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    public void load(String userId, Runnable onDone, Consumer<String> onError) {
        repository.load(userId, plan -> {
            applyPlan(plan);
            onDone.run();
        }, error -> onError.accept(error.getMessage()));
    }

    /** POLYMORPHISM: whatever subclass this is decides every answer below. */
    public Subscription getCurrent() {
        return current;
    }

    public void applyPlan(Subscription plan) {
        current = plan;
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public boolean canAddTask(int unfinishedCount) {
        return current.canAddTask(unfinishedCount);
    }

    public boolean isAvailable(Feature feature) {
        return feature.isAvailableOn(current);
    }

    public String limitMessage() {
        return "You've reached the Free limit of " + current.getMaxActiveTasks()
                + " unfinished tasks. Finish one, or upgrade to Pro for unlimited tasks.";
    }

    /** "You're using 12 of 15 free tasks." */
    public String usageText(int unfinishedCount) {
        if (current.isUnlimited()) {
            return "You're on " + current.getDisplayName() + ". Unlimited tasks, " + unfinishedCount + " open now.";
        }
        return "You're using " + unfinishedCount + " of " + current.getMaxActiveTasks() + " free tasks.";
    }

    /** One instance of each plan, cheapest first, for the Plans screen. */
    public List<Subscription> allPlans() {
        List<Subscription> plans = new ArrayList<>();
        plans.add(new FreePlan());
        plans.add(new ProPlan());
        plans.add(new MaxPlan());
        return plans;
    }

    /** The next plan up, or null when already on Max. */
    public PlanType recommendedUpgrade() {
        switch (current.getPlanType()) {
            case FREE:
                return PlanType.PRO;
            case PRO:
                return PlanType.MAX;
            default:
                return null;
        }
    }

    /**
     * POLYMORPHISM: the same call works with StripeCheckout (real test-mode
     * payment) or MockCheckout (demo mode). This method doesn't know which.
     */
    public void upgrade(Student student, PlanType target, CheckoutGateway gateway,
                        Runnable onDone, Consumer<String> onError) {
        if (target == current.getPlanType()) {
            onError.accept("You're already on " + current.getDisplayName() + ".");
            return;
        }
        gateway.checkout(student, target, onDone, onError);
    }
}
