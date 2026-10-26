package app.studybloom.ui.components;

import app.studybloom.ui.Dom;
import app.studybloom.web.Js;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

/** Five petals drift up from a completed card for about a second. Skipped with reduced motion. */
public final class PetalBurst {
    private static final String SVG = "<svg width=\"120\" height=\"70\" viewBox=\"0 0 120 70\" aria-hidden=\"true\">"
            + "<path d=\"M18 50c4-8 12-8 14 0-6 4-10 4-14 0z\" fill=\"#F472B6\"/>"
            + "<path d=\"M40 28c3-7 11-7 12 0-5 4-8 4-12 0z\" fill=\"#F9A8D4\"/>"
            + "<path d=\"M70 16c4-8 12-8 14 0-6 4-10 4-14 0z\" fill=\"#F472B6\"/>"
            + "<path d=\"M96 34c3-7 11-7 12 0-5 4-8 4-12 0z\" fill=\"#E9D5FF\"/>"
            + "<path d=\"M58 46c3-6 9-6 10 0-4 3-6 3-10 0z\" fill=\"#FBCFE8\"/></svg>";

    private PetalBurst() {
    }

    public static void play(HTMLElement card) {
        if (Js.prefersReducedMotion()) {
            return;
        }
        HTMLElement burst = Dom.svg("div", "petal-burst", SVG);
        card.appendChild(burst);
        Window.setTimeout(() -> {
            if (burst.getParentNode() != null) {
                burst.getParentNode().removeChild(burst);
            }
        }, 1200);
    }
}
