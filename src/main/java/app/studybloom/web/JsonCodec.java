package app.studybloom.web;

/** Converts a model to and from the JSON row Supabase (and localStorage) uses. */
public interface JsonCodec<T> {
    Json toJson(T item);

    T fromJson(Json json);

    String idOf(T item);
}
