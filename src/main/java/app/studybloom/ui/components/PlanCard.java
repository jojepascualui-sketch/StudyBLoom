package app.studybloom.ui.components;

import app.studybloom.model.PlanType;
import app.studybloom.model.Subscription;
import app.studybloom.ui.Component;
import app.studybloom.ui.Dom;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * One plan on the Plans screen. POLYMORPHISM: it is given any Subscription
 * subclass and reads the name, price and features from it.
 */
public class PlanCard extends Component {
    private final Subscription plan;
    private final boolean current;
    private final boolean recommended;
    private final String buttonLabel;
    private final Runnable onClick;

    public PlanCard(Subscription plan, boolean current, boolean recommended, String buttonLabel, Runnable onClick) {
        this.plan = plan;
        this.current = current;
        this.recommended = recommended;
        this.buttonLabel = buttonLabel;
        this.onClick = onClick;
    }

    @Override
    protected HTMLElement build() {
        boolean isMax = plan.getPlanType() == PlanType.MAX;
        String cls = "plan" + (current ? " current" : "") + (!current && recommended ? " pick" : "")
                + (!current && isMax ? " max" : "");
        HTMLElement card = Dom.el("section", cls);
        card.setAttribute("aria-label", plan.getDisplayName() + " plan");

        HTMLElement top = Dom.div("plan-top");
        HTMLElement price = Dom.text("div", "price", "₱" + plan.getMonthlyPricePesos());
        if (plan.getMonthlyPricePesos() > 0) {
            price.appendChild(Dom.text("small", "", " / month"));
        }
        Dom.append(top, Dom.text("h2", "plan-name" + (isMax ? " max-name" : ""), plan.getDisplayName()), price);

        HTMLElement list = Dom.el("ul", "");
        for (String feature : plan.getFeatures()) {
            list.appendChild(Dom.text("li", "", feature));
        }
        Dom.append(card, top, list);

        if (current) {
            HTMLElement row = Dom.div("plan-badge-row");
            row.appendChild(Dom.text("span", "badge", plan.isDemo() ? "Current plan (demo)" : "Current plan"));
            card.appendChild(row);
        } else if (buttonLabel != null) {
            String buttonClass = recommended ? "btn btn-primary" : isMax ? "btn btn-ghost btn-max" : "btn btn-ghost";
            card.appendChild(Dom.button(buttonClass, buttonLabel, onClick));
        }
        return card;
    }
}
