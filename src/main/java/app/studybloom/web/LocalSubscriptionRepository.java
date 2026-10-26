package app.studybloom.web;

import app.studybloom.model.PlanType;
import app.studybloom.model.Subscription;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.RepoError;
import app.studybloom.repo.SubscriptionRepository;
import java.util.function.Consumer;

/** Local demo mode: the plan is just remembered on this device. */
public class LocalSubscriptionRepository implements SubscriptionRepository {
    private static final String KEY = "sb.local.plan";

    private final KeyValueStore store;

    public LocalSubscriptionRepository(KeyValueStore store) {
        this.store = store;
    }

    @Override
    public void load(String userId, Consumer<Subscription> onOk, Consumer<RepoError> onError) {
        Subscription plan = Subscription.forPlan(PlanType.fromCode(store.get(KEY)));
        plan.setDemo(plan.getPlanType() != PlanType.FREE);
        onOk.accept(plan);
    }

    @Override
    public void activateDemoPlan(String userId, PlanType plan, Consumer<Subscription> onOk,
                                 Consumer<RepoError> onError) {
        store.set(KEY, plan.getCode());
        load(userId, onOk, onError);
    }
}
