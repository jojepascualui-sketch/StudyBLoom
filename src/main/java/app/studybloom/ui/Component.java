package app.studybloom.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * ABSTRACTION + INHERITANCE: every piece of UI (screens and reusable parts like
 * TaskCard or BloomMeter) extends this class and only says how to build its
 * element. Rendering and caching the element is shared here.
 */
public abstract class Component {
    private HTMLElement element;

    /** Subclasses build their DOM here. */
    protected abstract HTMLElement build();

    /** Builds a fresh element (used when the data changed). */
    public HTMLElement render() {
        element = build();
        return element;
    }

    /** The element built last time, building it on first use. */
    public HTMLElement getElement() {
        if (element == null) {
            render();
        }
        return element;
    }
}
