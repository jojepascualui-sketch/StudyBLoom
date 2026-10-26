package app.studybloom.service;

/**
 * A problem to show next to a form field. The message always says how to fix
 * it, as the design reference asks.
 */
public class ValidationError {
    public static final String FORM = "form";

    private final String field;
    private final String message;
    private final boolean planLimit;

    public ValidationError(String field, String message) {
        this(field, message, false);
    }

    public ValidationError(String field, String message, boolean planLimit) {
        this.field = field;
        this.message = message;
        this.planLimit = planLimit;
    }

    /** Which input the message belongs under ("title", "due", "email", "password", or "form"). */
    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    /** True when the fix is "upgrade your plan", so the UI can link to Plans. */
    public boolean isPlanLimit() {
        return planLimit;
    }

    @Override
    public String toString() {
        return field + ": " + message;
    }
}
