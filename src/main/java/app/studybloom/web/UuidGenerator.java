package app.studybloom.web;

import app.studybloom.repo.IdGenerator;

/** Random UUIDs from crypto.randomUUID(), matching the uuid id columns. */
public class UuidGenerator implements IdGenerator {
    @Override
    public String newId() {
        return Js.uuid();
    }
}
