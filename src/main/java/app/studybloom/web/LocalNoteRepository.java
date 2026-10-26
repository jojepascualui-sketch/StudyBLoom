package app.studybloom.web;

import app.studybloom.model.SubjectNote;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.NoteRepository;

public class LocalNoteRepository extends LocalListRepository<SubjectNote> implements NoteRepository {
    public LocalNoteRepository(KeyValueStore store) {
        super(store, "sb.local.notes", new NoteCodec());
    }
}
