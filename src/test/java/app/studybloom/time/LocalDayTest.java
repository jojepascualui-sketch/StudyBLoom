package app.studybloom.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.studybloom.fakes.FixedClock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LocalDayTest {

    @Test
    void matchesJavaTimeForManyDays() {
        LocalDate expected = LocalDate.of(1999, 12, 25);
        for (int i = 0; i < 20_000; i++) {
            LocalDay day = LocalDay.ofEpochDay(expected.toEpochDay());
            assertEquals(expected.getYear(), day.getYear());
            assertEquals(expected.getMonthValue(), day.getMonth());
            assertEquals(expected.getDayOfMonth(), day.getDay());
            assertEquals(expected.getDayOfWeek().getValue(), day.getDayOfWeek());
            assertEquals(expected.toEpochDay(), LocalDay.of(expected.getYear(), expected.getMonthValue(),
                    expected.getDayOfMonth()).getEpochDay());
            expected = expected.plusDays(1);
        }
    }

    @Test
    void weekRunsMondayToSunday() {
        LocalDay thursday = LocalDay.of(2026, 10, 29);
        assertEquals(LocalDay.of(2026, 10, 26), thursday.startOfWeek());
        assertEquals(LocalDay.of(2026, 11, 1), thursday.endOfWeek());
    }

    @Test
    void formatsLikeTheMockups() {
        FixedClock clock = new FixedClock();
        assertEquals("Monday, Oct 26", DateTexts.longDate(clock.today()));
        assertEquals("Sat, Oct 24", DateTexts.shortDate(LocalDay.of(2026, 10, 24)));
        assertEquals("11:59 PM", DateTexts.time(clock.at(0, 23, 59), clock));
        assertEquals("12:05 AM", DateTexts.time(0, 5));
        assertEquals("8:00 AM", DateTexts.time("08:00:00"));
    }

    @Test
    void localMidnightUsesTheTimeZone() {
        FixedClock clock = new FixedClock();
        long midnight = LocalDay.of(2026, 10, 26).startMillis(clock);
        // Manila midnight is 16:00 UTC the day before.
        assertEquals(java.time.Instant.parse("2026-10-25T16:00:00Z").toEpochMilli(), midnight);
        assertEquals(LocalDay.of(2026, 10, 26), LocalDay.of(midnight, clock));
        assertEquals(LocalDay.of(2026, 10, 25), LocalDay.of(midnight - 1, clock));
    }

    @Test
    void parsesDateInputValues() {
        assertEquals(LocalDay.of(2026, 10, 29), LocalDay.parseIso("2026-10-29"));
        assertNull(LocalDay.parseIso(""));
        assertNull(LocalDay.parseIso("not-a-date"));
    }
}
