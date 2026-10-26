package app.studybloom.ui.components;

import app.studybloom.ui.Component;
import app.studybloom.ui.Dom;
import app.studybloom.ui.NavTab;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.teavm.jso.dom.html.HTMLElement;

/** The bottom bar: Tasks, Schedule, Plans, Me. */
public class NavBar extends Component {
    private final Consumer<NavTab> onSelect;
    private final Map<NavTab, HTMLElement> buttons = new LinkedHashMap<>();

    public NavBar(Consumer<NavTab> onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    protected HTMLElement build() {
        HTMLElement nav = Dom.el("nav", "nav");
        nav.setAttribute("aria-label", "Main");
        for (NavTab tab : NavTab.values()) {
            HTMLElement button = Dom.el("button", "nav-item");
            button.setAttribute("type", "button");
            button.setInnerHTML(tab.getIcon());
            button.appendChild(Dom.text("span", "", tab.getLabel()));
            Dom.onClick(button, () -> onSelect.accept(tab));
            buttons.put(tab, button);
            nav.appendChild(button);
        }
        return nav;
    }

    public void setActive(NavTab active) {
        getElement().setHidden(active == null);
        for (Map.Entry<NavTab, HTMLElement> entry : buttons.entrySet()) {
            boolean on = entry.getKey() == active;
            Dom.toggleClass(entry.getValue(), "on", on);
            if (on) {
                entry.getValue().setAttribute("aria-current", "page");
            } else {
                entry.getValue().removeAttribute("aria-current");
            }
        }
    }
}
