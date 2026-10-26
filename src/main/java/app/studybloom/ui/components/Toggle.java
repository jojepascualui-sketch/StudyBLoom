package app.studybloom.ui.components;

import app.studybloom.ui.Component;
import app.studybloom.ui.Dom;
import java.util.function.Consumer;
import org.teavm.jso.dom.html.HTMLElement;

/** An on/off switch (role=switch) like the Dark mode and Demo mode rows. */
public class Toggle extends Component {
    private final String label;
    private final Consumer<Boolean> onChange;
    private boolean on;

    public Toggle(String label, boolean on, Consumer<Boolean> onChange) {
        this.label = label;
        this.on = on;
        this.onChange = onChange;
    }

    @Override
    protected HTMLElement build() {
        HTMLElement button = Dom.el("button", "switch");
        button.setAttribute("type", "button");
        button.setAttribute("role", "switch");
        button.setAttribute("aria-label", label);
        button.appendChild(Dom.el("span", "knob"));
        paint(button);
        Dom.onClick(button, () -> {
            on = !on;
            paint(button);
            onChange.accept(on);
        });
        return button;
    }

    public void setOn(boolean value) {
        on = value;
        paint(getElement());
    }

    private void paint(HTMLElement button) {
        button.setAttribute("aria-checked", on ? "true" : "false");
        Dom.toggleClass(button, "on", on);
    }
}
