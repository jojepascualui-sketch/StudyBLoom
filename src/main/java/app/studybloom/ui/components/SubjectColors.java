package app.studybloom.ui.components;

/** Gives each subject a stable tag color from the design palette. */
public final class SubjectColors {
    private static final String[] TONES = {"", "lav", "mint", "peach"};

    private SubjectColors() {
    }

    public static String toneFor(String subject) {
        if (subject == null) {
            return "";
        }
        int hash = 0;
        String key = subject.trim().toLowerCase();
        for (int i = 0; i < key.length(); i++) {
            hash = hash * 31 + key.charAt(i);
        }
        return TONES[Math.floorMod(hash, TONES.length)];
    }

    public static String tagClass(String subject) {
        String tone = toneFor(subject);
        return tone.isEmpty() ? "tag" : "tag " + tone;
    }
}
