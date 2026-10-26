package app.studybloom.web;

import app.studybloom.model.PlanType;
import app.studybloom.model.Student;
import app.studybloom.model.Subscription;
import app.studybloom.service.CheckoutGateway;
import java.util.function.Consumer;
import org.teavm.jso.browser.Window;

/**
 * POLYMORPHISM: the real CheckoutGateway. Sends the student to a Stripe
 * Payment Link (test mode). client_reference_id carries their user id, which
 * the stripe-webhook Edge Function uses to update the right subscription row.
 */
public class StripeCheckout implements CheckoutGateway {
    private final AppConfig config;

    public StripeCheckout(AppConfig config) {
        this.config = config;
    }

    @Override
    public void checkout(Student student, PlanType plan, Runnable onDone, Consumer<String> onError) {
        if (plan == PlanType.FREE) {
            onError.accept("To go back to Free, cancel from the link in your Stripe receipt email.");
            return;
        }
        String link = plan == PlanType.PRO ? config.getProPaymentLink() : config.getMaxPaymentLink();
        if (link.isEmpty()) {
            onError.accept("Payments aren't set up yet. Turn on Demo mode in Me to try "
                    + Subscription.forPlan(plan).getDisplayName() + ".");
            return;
        }
        String url = link + (link.contains("?") ? "&" : "?")
                + "client_reference_id=" + Window.encodeURIComponent(student.getId())
                + "&prefilled_email=" + Window.encodeURIComponent(student.getEmail());
        Js.redirect(url);
    }

    @Override
    public boolean isInstant() {
        return false;
    }
}
