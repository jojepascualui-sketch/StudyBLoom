package app.studybloom.ui.components;

import app.studybloom.ui.Component;
import app.studybloom.ui.Dom;
import org.teavm.jso.dom.html.HTMLElement;

/** A label, a control, and an error line under it in overdue red. */
public class FormField extends Component {
    private static int counter;

    private final String label;
    private final HTMLElement control;
    private final String id;
    private HTMLElement error;

    public FormField(String label, HTMLElement control) {
        this.label = label;
        this.control = control;
        this.id = "f" + (++counter);
    }

    @Override
    protected HTMLElement build() {
        HTMLElement field = Dom.div("field");
        HTMLElement labelEl = Dom.text("label", "", label);
        labelEl.setAttribute("for", id);
        control.setAttribute("id", id);
        control.setAttribute("aria-describedby", id + "-err");
        error = Dom.el("p", "field-error");
        error.setAttribute("id", id + "-err");
        error.setHidden(true);
        control.addEventListener("input", event -> clearError());
        Dom.append(field, labelEl, control, error);
        return field;
    }

    public void setError(String message) {
        getElement();
        error.setTextContent(message);
        error.setHidden(false);
        error.setClassName("field-error");
        control.setAttribute("aria-invalid", "true");
        control.getClassList().add("invalid");
    }

    /** A helpful, non-error note in the same spot (e.g. "Reset link sent"). */
    public void setInfo(String message) {
        getElement();
        error.setTextContent(message);
        error.setHidden(false);
        error.setClassName("field-error info");
    }

    public void clearError() {
        getElement();
        error.setHidden(true);
        control.removeAttribute("aria-invalid");
        control.getClassList().remove("invalid");
    }

    public HTMLElement getControl() {
        return control;
    }
}
