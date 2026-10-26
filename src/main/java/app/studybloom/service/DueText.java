package app.studybloom.service;

/** The due-date line on a task card, plus how to color it. */
public class DueText {
    public enum Style { NORMAL, OVERDUE, TODAY, DONE }

    private final String text;
    private final Style style;

    public DueText(String text, Style style) {
        this.text = text;
        this.style = style;
    }

    public String getText() {
        return text;
    }

    public Style getStyle() {
        return style;
    }
}
