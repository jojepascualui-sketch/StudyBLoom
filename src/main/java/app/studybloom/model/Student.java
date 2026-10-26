package app.studybloom.model;

/**
 * The signed-in student. ENCAPSULATION: the display name is derived inside the
 * class, so screens just ask for {@link #getFirstName()}.
 */
public class Student {
    private final String id;
    private final String email;
    private String displayName;

    public Student(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** "bea.santos@school.edu.ph" becomes "Bea" when no display name is set. */
    public String getFirstName() {
        String source = displayName != null && !displayName.trim().isEmpty() ? displayName : localPart();
        String first = firstWord(source.trim());
        if (first.isEmpty()) {
            return "there";
        }
        return Character.toUpperCase(first.charAt(0)) + first.substring(1).toLowerCase();
    }

    public String getInitial() {
        return getFirstName().substring(0, 1).toUpperCase();
    }

    private static String firstWord(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (" ._-+".indexOf(text.charAt(i)) >= 0) {
                return text.substring(0, i);
            }
        }
        return text;
    }

    private String localPart() {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
