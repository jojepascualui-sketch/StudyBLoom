package app.studybloom.service;

import app.studybloom.model.PlanType;
import app.studybloom.model.Student;
import java.util.function.Consumer;

/**
 * ABSTRACTION: "move this student to that plan". StripeCheckout sends them to a
 * Stripe test-mode payment page; MockCheckout switches the plan instantly.
 */
public interface CheckoutGateway {
    void checkout(Student student, PlanType plan, Runnable onDone, Consumer<String> onError);

    /** True when the plan changes right away (no redirect to a payment page). */
    boolean isInstant();
}
