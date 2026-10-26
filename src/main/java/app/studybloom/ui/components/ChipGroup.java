package app.studybloom.ui.components;

import app.studybloom.ui.Component;
import app.studybloom.ui.Dom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.teavm.jso.dom.html.HTMLElement;

/** A row of pill chips where one can be picked (like radio buttons). */
public class ChipGroup extends Component {
    private final String label;
    private final List<String> values = new ArrayList<>();
    private final Map<String, HTMLElement> chips = new LinkedHashMap<>();
    private final Consumer<String> onPick;
    private HTMLElement row;
    private HTMLElement extra;
    private String selected;

    public ChipGroup(String label, List<String> values, Consumer<String> onPick) {
        this.label = label;
        this.values.addAll(values);
        this.onPick = onPick;
    }

    @Override
    protected HTMLElement build() {
        row = Dom.div("chips");
        row.setAttribute("role", "radiogroup");
        row.setAttribute("aria-label", label);
        chips.clear();
        for (String value : values) {
            row.appendChild(makeChip(value));
        }
        if (extra != null) {
            row.appendChild(extra);
        }
        return row;
    }

    /** A trailing chip that is a plain action (e.g. "+ New"), not a choice. */
    public void setExtraButton(String text, Runnable action) {
        extra = Dom.button("chip chip-add", text, action);
    }

    public void addValue(String value) {
        if (!values.contains(value)) {
            values.add(value);
            HTMLElement chip = makeChip(value);
            getElement();
            if (extra != null) {
                row.insertBefore(chip, extra);
            } else {
                row.appendChild(chip);
            }
        }
    }

    public String getSelected() {
        return selected;
    }

    public void select(String value) {
        selected = value;
        for (Map.Entry<String, HTMLElement> entry : chips.entrySet()) {
            boolean on = entry.getKey().equals(value);
            Dom.toggleClass(entry.getValue(), "on", on);
            entry.getValue().setAttribute("aria-checked", on ? "true" : "false");
        }
    }

    private HTMLElement makeChip(String value) {
        HTMLElement chip = Dom.button("chip", value, () -> {
            select(value);
            onPick.accept(value);
        });
        chip.setAttribute("role", "radio");
        chip.setAttribute("aria-checked", "false");
        chips.put(value, chip);
        return chip;
    }
}
