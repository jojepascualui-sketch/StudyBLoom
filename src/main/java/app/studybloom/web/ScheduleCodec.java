package app.studybloom.web;

import app.studybloom.model.ScheduleEntry;

/** Maps ScheduleEntry to the schedules table. */
public class ScheduleCodec implements JsonCodec<ScheduleEntry> {
    @Override
    public Json toJson(ScheduleEntry entry) {
        return Json.object()
                .put("id", entry.getId())
                .put("user_id", entry.getUserId())
                .put("subject", entry.getSubject())
                .put("day_of_week", entry.getDayOfWeek())
                .put("start_time", entry.getStartTime())
                .put("end_time", entry.getEndTime())
                .put("room", entry.getRoom());
    }

    @Override
    public ScheduleEntry fromJson(Json json) {
        ScheduleEntry entry = new ScheduleEntry();
        entry.setId(json.getString("id"));
        entry.setUserId(json.getString("user_id"));
        entry.setSubject(json.getString("subject"));
        entry.setDayOfWeek((int) json.getNumber("day_of_week"));
        entry.setStartTime(json.getString("start_time"));
        entry.setEndTime(json.getString("end_time"));
        entry.setRoom(json.getString("room"));
        return entry;
    }

    @Override
    public String idOf(ScheduleEntry entry) {
        return entry.getId();
    }
}
