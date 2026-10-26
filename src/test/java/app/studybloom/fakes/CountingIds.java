package app.studybloom.fakes;

import app.studybloom.repo.IdGenerator;

public class CountingIds implements IdGenerator {
    private int next = 1;

    @Override
    public String newId() {
        return "task-" + next++;
    }
}
