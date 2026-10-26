package app.studybloom.repo;

/** Makes record ids on the device, so a task created offline already has its final id. */
public interface IdGenerator {
    String newId();
}
