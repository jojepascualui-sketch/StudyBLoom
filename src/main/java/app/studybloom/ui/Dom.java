package app.studybloom.ui;

import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLTextAreaElement;

/** Tiny helpers for building DOM elements from Java. */
public final class Dom {
    private Dom() {
    }

    public static HTMLDocument doc() {
        return HTMLDocument.current();
    }

    public static HTMLElement el(String tag, String className) {
        HTMLElement element = doc().createElement(tag);
        if (className != null && !className.isEmpty()) {
            element.setClassName(className);
        }
        return element;
    }

    public static HTMLElement div(String className) {
        return el("div", className);
    }

    public static HTMLElement text(String tag, String className, String text) {
        HTMLElement element = el(tag, className);
        element.setTextContent(text);
        return element;
    }

    /** For trusted, hard-coded SVG markup only (icons, the flower). Never user text. */
    public static HTMLElement svg(String tag, String className, String markup) {
        HTMLElement element = el(tag, className);
        element.setInnerHTML(markup);
        return element;
    }

    public static HTMLElement button(String className, String label, Runnable onClick) {
        HTMLElement button = el("button", className);
        button.setAttribute("type", "button");
        button.setTextContent(label);
        onClick(button, onClick);
        return button;
    }

    public static void onClick(HTMLElement element, Runnable action) {
        element.addEventListener("click", event -> action.run());
    }

    /** Enter or Space on a non-button element acts like a click. */
    public static void onActivate(HTMLElement element, Runnable action) {
        onClick(element, action);
        element.addEventListener("keydown", event -> {
            String key = ((KeyboardEvent) event).getKey();
            if ("Enter".equals(key) || " ".equals(key)) {
                event.preventDefault();
                action.run();
            }
        });
    }

    public static HTMLInputElement input(String type, String className) {
        HTMLInputElement input = (HTMLInputElement) el("input", className);
        input.setType(type);
        return input;
    }

    public static HTMLTextAreaElement textarea(String className) {
        return (HTMLTextAreaElement) el("textarea", className);
    }

    public static void clear(HTMLElement element) {
        element.setInnerHTML("");
    }

    public static void append(HTMLElement parent, HTMLElement... children) {
        for (HTMLElement child : children) {
            if (child != null) {
                parent.appendChild(child);
            }
        }
    }

    public static void toggleClass(HTMLElement element, String className, boolean on) {
        if (on) {
            element.getClassList().add(className);
        } else {
            element.getClassList().remove(className);
        }
    }
}
