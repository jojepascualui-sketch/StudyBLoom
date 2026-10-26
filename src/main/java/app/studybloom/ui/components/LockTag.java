package app.studybloom.ui.components;

import app.studybloom.model.PlanType;
import app.studybloom.ui.Dom;
import org.teavm.jso.dom.html.HTMLElement;

/** The small "Pro" or "Max" tag on a locked feature. Tapping it opens Plans. */
public final class LockTag {
    private LockTag() {
    }

    public static HTMLElement create(PlanType plan, Runnable openPlans) {
        String name = plan == PlanType.MAX ? "Max" : "Pro";
        HTMLElement tag = Dom.button("tag lock" + (plan == PlanType.MAX ? " lav" : ""), name, openPlans);
        tag.setAttribute("aria-label", "Needs " + name + ". See plans");
        return tag;
    }
}
