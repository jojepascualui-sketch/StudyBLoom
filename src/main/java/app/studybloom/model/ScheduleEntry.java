package app.studybloom.model;

/** One class meeting in the weekly schedule (Max plan). */
public class ScheduleEntry {
    private String id;
    private String userId;
    private String subject;
    private int dayOfWeek;
    private String startTime;
    private String endTime;
    private String room;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject == null ? null : subject.trim();
    }

    /** 1 = Monday ... 7 = Sunday. */
    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be 1..7");
        }
        this.dayOfWeek = dayOfWeek;
    }

    /** "HH:mm", 24-hour. */
    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = trimSeconds(startTime);
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = trimSeconds(endTime);
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room == null || room.trim().isEmpty() ? null : room.trim();
    }

    /** Postgres returns "08:00:00"; the app works with "08:00". */
    private static String trimSeconds(String time) {
        if (time != null && time.length() > 5) {
            return time.substring(0, 5);
        }
        return time;
    }
}
