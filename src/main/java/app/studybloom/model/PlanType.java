package app.studybloom.model;

/** The three plans, in order from cheapest to most features. */
public enum PlanType {
    FREE("free"),
    PRO("pro"),
    MAX("max");

    private final String code;

    PlanType(String code) {
        this.code = code;
    }

    /** The value stored in the subscriptions.plan column. */
    public String getCode() {
        return code;
    }

    public boolean isAtLeast(PlanType other) {
        return ordinal() >= other.ordinal();
    }

    public static PlanType fromCode(String code) {
        for (PlanType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return FREE;
    }
}
