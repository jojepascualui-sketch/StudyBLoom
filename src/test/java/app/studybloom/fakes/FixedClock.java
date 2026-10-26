package app.studybloom.fakes;

import app.studybloom.time.AppClock;
import app.studybloom.time.LocalDay;

/** A clock frozen at a chosen local time in Manila (UTC+8). */
public class FixedClock implements AppClock {
    public static final int MANILA = 8 * 60;

    private long now;

    /** Monday, Oct 26 2026, 10:00 AM in Manila. */
    public FixedClock() {
        this(LocalDay.of(2026, 10, 26), 10, 0);
    }

    public FixedClock(LocalDay day, int hour, int minute) {
        now = day.atTime(hour, minute, this);
    }

    @Override
    public long nowMillis() {
        return now;
    }

    @Override
    public int offsetMinutes(long atMillis) {
        return MANILA;
    }

    public void advanceMinutes(long minutes) {
        now += minutes * 60_000L;
    }

    /** Instant of hour:minute, {@code days} days from today. */
    public long at(int days, int hour, int minute) {
        return today().plusDays(days).atTime(hour, minute, this);
    }
}
