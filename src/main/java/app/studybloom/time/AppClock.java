package app.studybloom.time;

/**
 * ABSTRACTION: where "now" and the time zone come from. The browser uses
 * BrowserClock; unit tests use a fixed clock so dates are predictable.
 */
public interface AppClock {
    long nowMillis();

    /** Minutes to add to UTC to get local time at the given instant (+480 in Manila). */
    int offsetMinutes(long atMillis);

    default LocalDay today() {
        return LocalDay.of(nowMillis(), this);
    }
}
