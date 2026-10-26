package app.studybloom.web;

import app.studybloom.model.SubjectNote;

/** Maps SubjectNote to the notes table. */
public class NoteCodec implements JsonCodec<SubjectNote> {
    @Override
    public Json toJson(SubjectNote note) {
        return Json.object()
                .put("id", note.getId())
                .put("user_id", note.getUserId())
                .put("subject", note.getSubject())
                .put("body", note.getBody())
                .putTimestamp("updated_at", note.getUpdatedAt());
    }

    @Override
    public SubjectNote fromJson(Json json) {
        SubjectNote note = new SubjectNote();
        note.setId(json.getString("id"));
        note.setUserId(json.getString("user_id"));
        note.setSubject(json.getString("subject"));
        note.setBody(json.getString("body"));
        note.setUpdatedAt(json.getTimestamp("updated_at"));
        return note;
    }

    @Override
    public String idOf(SubjectNote note) {
        return note.getId();
    }
}
