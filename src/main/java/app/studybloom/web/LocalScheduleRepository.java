package app.studybloom.web;

import app.studybloom.model.ScheduleEntry;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.ScheduleRepository;

public class LocalScheduleRepository extends LocalListRepository<ScheduleEntry> implements ScheduleRepository {
    public LocalScheduleRepository(KeyValueStore store) {
        super(store, "sb.local.schedules", new ScheduleCodec());
    }
}
