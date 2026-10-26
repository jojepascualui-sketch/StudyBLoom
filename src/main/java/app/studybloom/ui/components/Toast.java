package app.studybloom.ui.components;

import app.studybloom.ui.Component;
import app.studybloom.ui.Dom;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

/** The rose message bar above the nav, with an optional action such as Undo. */
public class Toast extends Component {
    public static final int UNDO_MS = 5000;

    private HTMLElement message;
    private HTMLElement action;
    private Runnable onAction;
    private Runnable onExpire;
    private int timer = -1;

    @Override
    protected HTMLElement build() {
        HTMLElement toast = Dom.div("toast");
        toast.setAttribute("role", "status");
        toast.setAttribute("aria-live", "polite");
        toast.setHidden(true);
        message = Dom.el("span", "toast-msg");
        action = Dom.button("toast-action", "", () -> {
            Runnable run = onAction;
            onExpire = null;
            hide();
            if (run != null) {
                run.run();
            }
        });
        Dom.append(toast, message, action);
        return toast;
    }

    public void show(String text) {
        show(text, null, null, 3500, null);
    }

    /**
     * Shows a message. If another toast with a pending onExpire is showing,
     * that callback runs first, so nothing is lost.
     */
    public void show(String text, String actionLabel, Runnable actionHandler, int millis, Runnable expire) {
        flush();
        HTMLElement toast = getElement();
        message.setTextContent(text);
        action.setTextContent(actionLabel == null ? "" : actionLabel);
        action.setHidden(actionLabel == null);
        onAction = actionHandler;
        onExpire = expire;
        toast.setHidden(false);
        toast.getClassList().remove("in");
        Window.requestAnimationFrame(t -> toast.getClassList().add("in"));
        timer = Window.setTimeout(() -> {
            timer = -1;
            flush();
        }, millis);
    }

    /** Ends the current toast now, running its onExpire if it has one. */
    public void flush() {
        Runnable expire = onExpire;
        onExpire = null;
        hide();
        if (expire != null) {
            expire.run();
        }
    }

    private void hide() {
        if (timer >= 0) {
            Window.clearTimeout(timer);
            timer = -1;
        }
        HTMLElement toast = getElement();
        toast.getClassList().remove("in");
        toast.setHidden(true);
        onAction = null;
    }
}
