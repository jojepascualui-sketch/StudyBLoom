package app.studybloom.service;

/** What the bloom meter shows: how many petals are filled and the two lines of text. */
public class BloomProgress {
    public static final int PETALS = 5;

    private final int done;
    private final int total;
    private final int filledPetals;
    private final String headline;
    private final String message;

    public BloomProgress(int done, int total, int filledPetals, String headline, String message) {
        this.done = done;
        this.total = total;
        this.filledPetals = filledPetals;
        this.headline = headline;
        this.message = message;
    }

    public int getDone() {
        return done;
    }

    public int getTotal() {
        return total;
    }

    public int getFilledPetals() {
        return filledPetals;
    }

    public String getHeadline() {
        return headline;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFullBloom() {
        return filledPetals == PETALS;
    }

    /** "3/5" in the middle of the flower. */
    public String getCenterLabel() {
        return done + "/" + total;
    }
}
