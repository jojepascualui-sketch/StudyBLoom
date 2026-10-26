package app.studybloom.web;

import app.studybloom.model.SubjectNote;
import app.studybloom.repo.NoteRepository;

public class SupabaseNoteRepository extends SupabaseRestRepository<SubjectNote> implements NoteRepository {
    public SupabaseNoteRepository(SupabaseClient client) {
        super(client, "notes", "subject.asc", new NoteCodec());
    }
}
