package app.studybloom.time;

/** Formats days and times the way the mockups write them. */
public final class DateTexts {
    private static final String[] WEEKDAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private DateTexts() {
    }

    /** "Monday, Oct 26" */
    public static String longDate(LocalDay day) {
        return weekday(day.getDayOfWeek()) + ", " + MONTHS[day.getMonth() - 1] + " " + day.getDay();
    }

    /** "Thu, Oct 29" */
    public static String shortDate(LocalDay day) {
        return shortWeekday(day.getDayOfWeek()) + ", " + MONTHS[day.getMonth() - 1] + " " + day.getDay();
    }

    /** "11:59 PM" for an instant. */
    public static String time(long millis, AppClock clock) {
        long local = millis + clock.offsetMinutes(millis) * 60_000L;
        int minuteOfDay = (int) (Math.floorMod(local, 86_400_000L) / 60_000L);
        return time(minuteOfDay / 60, minuteOfDay % 60);
    }

    /** "8:00 AM" for "08:00". */
    public static String time(String hhmm) {
        if (hhmm == null || hhmm.length() < 5) {
            return "";
        }
        return time(Integer.parseInt(hhmm.substring(0, 2)), Integer.parseInt(hhmm.substring(3, 5)));
    }

    public static String time(int hour, int minute) {
        String suffix = hour < 12 ? "AM" : "PM";
        int h = hour % 12 == 0 ? 12 : hour % 12;
        return h + ":" + LocalDay.pad(minute) + " " + suffix;
    }

    public static String weekday(int dayOfWeek) {
        return WEEKDAYS[dayOfWeek - 1];
    }

    public static String shortWeekday(int dayOfWeek) {
        return WEEKDAYS[dayOfWeek - 1].substring(0, 3);
    }
}
