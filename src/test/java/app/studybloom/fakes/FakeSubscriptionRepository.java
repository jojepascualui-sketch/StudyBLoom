package app.studybloom.fakes;

import app.studybloom.model.PlanType;
import app.studybloom.model.Subscription;
import app.studybloom.repo.RepoError;
import app.studybloom.repo.SubscriptionRepository;
import java.util.function.Consumer;

public class FakeSubscriptionRepository implements SubscriptionRepository {
    private PlanType plan = PlanType.FREE;
    private boolean demoAllowed = true;

    public void setPlan(PlanType plan) {
        this.plan = plan;
    }

    public PlanType getPlan() {
        return plan;
    }

    public void setDemoAllowed(boolean demoAllowed) {
        this.demoAllowed = demoAllowed;
    }

    @Override
    public void load(String userId, Consumer<Subscription> onOk, Consumer<RepoError> onError) {
        onOk.accept(Subscription.forPlan(plan));
    }

    @Override
    public void activateDemoPlan(String userId, PlanType newPlan, Consumer<Subscription> onOk,
                                 Consumer<RepoError> onError) {
        if (!demoAllowed) {
            onError.accept(RepoError.of("Demo mode is turned off on the server."));
            return;
        }
        plan = newPlan;
        Subscription subscription = Subscription.forPlan(newPlan);
        subscription.setDemo(true);
        onOk.accept(subscription);
    }
}
