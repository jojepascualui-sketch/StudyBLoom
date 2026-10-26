package app.studybloom.web;

import app.studybloom.model.PlanType;
import app.studybloom.model.Subscription;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.RepoError;
import app.studybloom.repo.SubscriptionRepository;
import java.util.function.Consumer;

/**
 * Reads the subscriptions row. The browser can never write it: RLS only allows
 * SELECT, and plan changes come from the Stripe webhook or the demo RPC.
 */
public class SupabaseSubscriptionRepository implements SubscriptionRepository {
    private static final String CACHE_KEY = "sb.plan.cache";

    private final SupabaseClient client;
    private final KeyValueStore store;

    public SupabaseSubscriptionRepository(SupabaseClient client, KeyValueStore store) {
        this.client = client;
        this.store = store;
    }

    @Override
    public void load(String userId, Consumer<Subscription> onOk, Consumer<RepoError> onError) {
        String path = "/rest/v1/subscriptions?select=plan,status,current_period_end,is_demo&user_id=eq." + userId;
        client.rest("GET", path, null, null, body -> {
            Json rows = Json.parse(body);
            Json row = rows.size() > 0 ? rows.at(0) : Json.object().put("plan", "free");
            store.set(CACHE_KEY, row.stringify());
            onOk.accept(toSubscription(row));
        }, error -> {
            String cached = store.get(CACHE_KEY);
            if (error.isOffline() && cached != null) {
                onOk.accept(toSubscription(Json.parse(cached)));
            } else {
                onError.accept(error);
            }
        });
    }

    @Override
    public void activateDemoPlan(String userId, PlanType plan, Consumer<Subscription> onOk,
                                 Consumer<RepoError> onError) {
        Json body = Json.object().put("new_plan", plan.getCode());
        client.rest("POST", "/rest/v1/rpc/demo_set_plan", body.stringify(), null,
                ok -> load(userId, onOk, onError), error -> {
                    String message = error.getMessage() != null && error.getMessage().contains("Demo mode")
                            ? error.getMessage()
                            : "Couldn't switch plans: " + error.getMessage();
                    onError.accept(new RepoError(message, error.isOffline(), error.getStatus()));
                });
    }

    /** POLYMORPHISM: the factory returns FreePlan, ProPlan or MaxPlan. */
    private static Subscription toSubscription(Json row) {
        String status = row.has("status") ? row.getString("status") : "active";
        long periodEnd = row.getTimestamp("current_period_end");
        boolean active = ("active".equals(status) || "trialing".equals(status))
                && (periodEnd == 0 || periodEnd > (long) Js.now());
        PlanType type = active ? PlanType.fromCode(row.getString("plan")) : PlanType.FREE;
        Subscription subscription = Subscription.forPlan(type);
        subscription.setStatus(status);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setDemo(row.getBoolean("is_demo"));
        return subscription;
    }
}
