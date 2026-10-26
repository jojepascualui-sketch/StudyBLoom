package app.studybloom.service;

import app.studybloom.model.PlanType;
import app.studybloom.model.Student;
import app.studybloom.repo.SubscriptionRepository;
import java.util.function.Consumer;

/**
 * Demo mode checkout: upgrades without payment so Pro and Max can be shown in
 * class. The database only accepts this while demo mode is switched on there
 * (see the demo_set_plan function in the migrations).
 */
public class MockCheckout implements CheckoutGateway {
    private final SubscriptionRepository repository;
    private final SubscriptionService subscriptions;

    public MockCheckout(SubscriptionRepository repository, SubscriptionService subscriptions) {
        this.repository = repository;
        this.subscriptions = subscriptions;
    }

    @Override
    public void checkout(Student student, PlanType plan, Runnable onDone, Consumer<String> onError) {
        repository.activateDemoPlan(student.getId(), plan, newPlan -> {
            subscriptions.applyPlan(newPlan);
            onDone.run();
        }, error -> onError.accept(error.getMessage()));
    }

    @Override
    public boolean isInstant() {
        return true;
    }
}
