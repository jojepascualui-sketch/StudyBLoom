package app.studybloom.ui.screens;

import app.studybloom.AppServices;
import app.studybloom.model.PlanType;
import app.studybloom.model.Subscription;
import app.studybloom.service.CheckoutGateway;
import app.studybloom.service.SubscriptionService;
import app.studybloom.ui.App;
import app.studybloom.ui.Dom;
import app.studybloom.ui.NavTab;
import app.studybloom.ui.Screen;
import app.studybloom.ui.components.PlanCard;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screen 5: Plans. Current plan is outlined softly, the recommended upgrade
 * gets the petal border, Max gets the pink-to-lavender border.
 */
public class PricingScreen extends Screen {
    public PricingScreen(App app) {
        super(app);
    }

    @Override
    public NavTab getTab() {
        return NavTab.PLANS;
    }

    @Override
    public String getTitle() {
        return "Plans";
    }

    @Override
    protected HTMLElement build() {
        AppServices services = app().services();
        SubscriptionService subscriptions = services.getSubscriptions();
        Subscription current = subscriptions.getCurrent();
        PlanType recommended = subscriptions.recommendedUpgrade();
        CheckoutGateway checkout = services.getCheckout();

        HTMLElement page = Dom.div("plans-screen");
        Dom.append(page, Dom.text("h1", "h-title", "Plans"),
                Dom.text("p", "h-sub plans-usage", subscriptions.usageText(services.getTasks().countUnfinished())));

        if (checkout.isInstant()) {
            page.appendChild(Dom.text("p", "demo-note", services.isLocalMode()
                    ? "Local demo mode: plan changes are free and instant."
                    : "Demo mode is on: plan changes are free and instant. Turn it off in Me for real (test-mode) checkout."));
        }

        // POLYMORPHISM: each card gets a different Subscription subclass.
        for (Subscription plan : subscriptions.allPlans()) {
            PlanType type = plan.getPlanType();
            boolean isCurrent = type == current.getPlanType();
            if (isCurrent) {
                plan.setDemo(current.isDemo());
            }
            String label = null;
            if (!isCurrent && type.isAtLeast(current.getPlanType())) {
                label = "Upgrade to " + plan.getDisplayName();
            } else if (!isCurrent && checkout.isInstant()) {
                label = "Switch to " + plan.getDisplayName();
            }
            page.appendChild(new PlanCard(plan, isCurrent, type == recommended, label,
                    () -> choose(type, plan.getDisplayName())).render());
        }

        if (!checkout.isInstant()) {
            page.appendChild(Dom.text("p", "fine-print",
                    "Payments run on Stripe in test mode. Use card 4242 4242 4242 4242, any future date and any CVC."));
        }
        return page;
    }

    private void choose(PlanType type, String name) {
        AppServices services = app().services();
        services.getSubscriptions().upgrade(services.getAuth().getCurrentStudent(), type, services.getCheckout(),
                () -> app().toast().show("You're on " + name + " now. Enjoy!"),
                error -> app().toast().show(error));
    }
}
