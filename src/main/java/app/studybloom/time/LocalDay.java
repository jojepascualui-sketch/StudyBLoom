package app.studybloom.time;

/**
 * A calendar day in the student's time zone. TeaVM has no java.time, so this
 * small immutable value class does the date math the app needs.
 */
public final class LocalDay implements Comparable<LocalDay> {
    private static final long DAY_MS = 86_400_000L;

    private final long epochDay;
    private final int year;
    private final int month;
    private final int day;

    private LocalDay(long epochDay) {
        this.epochDay = epochDay;
        // Civil-from-days algorithm (Howard Hinnant), valid for all Gregorian dates.
        long z = epochDay + 719_468;
        long era = (z >= 0 ? z : z - 146_096) / 146_097;
        long doe = z - era * 146_097;
        long yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        int d = (int) (doy - (153 * mp + 2) / 5 + 1);
        int m = (int) (mp < 10 ? mp + 3 : mp - 9);
        this.year = (int) (yoe + era * 400 + (m <= 2 ? 1 : 0));
        this.month = m;
        this.day = d;
    }

    public static LocalDay ofEpochDay(long epochDay) {
        return new LocalDay(epochDay);
    }

    public static LocalDay of(int year, int month, int day) {
        int y = month <= 2 ? year - 1 : year;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153L * (month > 2 ? month - 3 : month + 9) + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return new LocalDay(era * 146_097 + doe - 719_468);
    }

    /** The local day that contains the given instant. */
    public static LocalDay of(long millis, AppClock clock) {
        long local = millis + clock.offsetMinutes(millis) * 60_000L;
        return new LocalDay(Math.floorDiv(local, DAY_MS));
    }

    /** Parses "2026-10-29" (the value of an input type=date). */
    public static LocalDay parseIso(String text) {
        if (text == null || text.length() < 10) {
            return null;
        }
        try {
            int y = Integer.parseInt(text.substring(0, 4));
            int m = Integer.parseInt(text.substring(5, 7));
            int d = Integer.parseInt(text.substring(8, 10));
            return of(y, m, d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The instant this day starts (local midnight). */
    public long startMillis(AppClock clock) {
        return atTime(0, 0, clock);
    }

    /** The instant of hour:minute on this day, local time. */
    public long atTime(int hour, int minute, AppClock clock) {
        long localMillis = epochDay * DAY_MS + (hour * 60L + minute) * 60_000L;
        int offset = clock.offsetMinutes(localMillis);
        return localMillis - offset * 60_000L;
    }

    public LocalDay plusDays(int days) {
        return new LocalDay(epochDay + days);
    }

    /** 1 = Monday ... 7 = Sunday. 1970-01-01 was a Thursday. */
    public int getDayOfWeek() {
        return (int) Math.floorMod(epochDay + 3, 7L) + 1;
    }

    public LocalDay startOfWeek() {
        return plusDays(1 - getDayOfWeek());
    }

    public LocalDay endOfWeek() {
        return plusDays(7 - getDayOfWeek());
    }

    public long getEpochDay() {
        return epochDay;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public boolean isBefore(LocalDay other) {
        return epochDay < other.epochDay;
    }

    public boolean isAfter(LocalDay other) {
        return epochDay > other.epochDay;
    }

    public String toIsoString() {
        return year + "-" + pad(month) + "-" + pad(day);
    }

    @Override
    public int compareTo(LocalDay other) {
        return Long.compare(epochDay, other.epochDay);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LocalDay && ((LocalDay) o).epochDay == epochDay;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(epochDay);
    }

    @Override
    public String toString() {
        return toIsoString();
    }

    static String pad(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }
}
