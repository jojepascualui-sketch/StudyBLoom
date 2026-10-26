package app.studybloom.ui.components;

import app.studybloom.ui.Component;
import app.studybloom.ui.Dom;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLElement;

/** A sheet that slides up over the current screen, with a dimmed scrim behind it. */
public class BottomSheet extends Component {
    private final HTMLElement content;
    private final String label;
    private final Runnable onClosed;
    private HTMLElement previousFocus;

    public BottomSheet(HTMLElement content, String label, Runnable onClosed) {
        this.content = content;
        this.label = label;
        this.onClosed = onClosed;
    }

    @Override
    protected HTMLElement build() {
        HTMLElement layer = Dom.div("sheet-layer");
        HTMLElement scrim = Dom.div("scrim");
        Dom.onClick(scrim, this::close);

        HTMLElement sheet = Dom.div("sheet");
        sheet.setAttribute("role", "dialog");
        sheet.setAttribute("aria-modal", "true");
        sheet.setAttribute("aria-label", label);
        sheet.addEventListener("keydown", event -> {
            if ("Escape".equals(((KeyboardEvent) event).getKey())) {
                close();
            }
        });
        Dom.append(sheet, Dom.div("grab"), content);
        Dom.append(layer, scrim, sheet);
        return layer;
    }

    public void open(HTMLElement host) {
        previousFocus = Dom.doc().getActiveElement();
        HTMLElement layer = render();
        host.appendChild(layer);
        Dom.doc().getBody().getClassList().add("sheet-open");
        Window.requestAnimationFrame(t -> {
            layer.getClassList().add("in");
            HTMLElement first = (HTMLElement) layer.querySelector("input, textarea, button");
            if (first != null) {
                first.focus();
            }
        });
    }

    public void close() {
        HTMLElement layer = getElement();
        if (layer.getParentNode() == null) {
            return;
        }
        layer.getParentNode().removeChild(layer);
        Dom.doc().getBody().getClassList().remove("sheet-open");
        if (previousFocus != null) {
            previousFocus.focus();
        }
        if (onClosed != null) {
            onClosed.run();
        }
    }
}
