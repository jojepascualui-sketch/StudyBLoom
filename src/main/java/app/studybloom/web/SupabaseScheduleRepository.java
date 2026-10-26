package app.studybloom.web;

import app.studybloom.model.ScheduleEntry;
import app.studybloom.repo.ScheduleRepository;

public class SupabaseScheduleRepository extends SupabaseRestRepository<ScheduleEntry> implements ScheduleRepository {
    public SupabaseScheduleRepository(SupabaseClient client) {
        super(client, "schedules", "day_of_week.asc,start_time.asc", new ScheduleCodec());
    }
}
