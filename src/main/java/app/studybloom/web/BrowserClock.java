package app.studybloom.web;

import app.studybloom.time.AppClock;

/** The real clock and time zone of the student's device. */
public class BrowserClock implements AppClock {
    @Override
    public long nowMillis() {
        return (long) Js.now();
    }

    @Override
    public int offsetMinutes(long atMillis) {
        return Js.offsetMinutes(atMillis);
    }
}
