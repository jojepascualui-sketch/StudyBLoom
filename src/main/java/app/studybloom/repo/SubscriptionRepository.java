package app.studybloom.repo;

import app.studybloom.model.PlanType;
import app.studybloom.model.Subscription;
import java.util.function.Consumer;

/** Reads the student's plan. Only the server (Stripe webhook or demo RPC) can change it. */
public interface SubscriptionRepository {
    void load(String userId, Consumer<Subscription> onOk, Consumer<RepoError> onError);

    /** Demo mode only: switch plan without paying. The database refuses this unless demo mode is enabled there. */
    void activateDemoPlan(String userId, PlanType plan, Consumer<Subscription> onOk, Consumer<RepoError> onError);
}
