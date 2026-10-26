package app.studybloom.web;

import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.RepoError;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Talks to Supabase over HTTP: Auth (GoTrue) for sign-in and PostgREST for
 * data. Holds the session and refreshes the access token when it expires.
 *
 * Only the public anon key is used here. Row Level Security in the database
 * decides what each signed-in student may read or change.
 */
public class SupabaseClient {
    private static final String SESSION_KEY = "sb.session";

    private final String url;
    private final String anonKey;
    private final HttpClient http;
    private final KeyValueStore store;
    private final List<Runnable> waitingForRefresh = new ArrayList<>();
    private Json session;
    private boolean refreshing;
    private Runnable onSessionExpired = () -> { };

    public SupabaseClient(String url, String anonKey, HttpClient http, KeyValueStore store) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.anonKey = anonKey;
        this.http = http;
        this.store = store;
        String saved = store.get(SESSION_KEY);
        if (saved != null) {
            Json parsed = Json.parse(saved);
            session = parsed.has("access_token") ? parsed : null;
        }
    }

    public void setOnSessionExpired(Runnable onSessionExpired) {
        this.onSessionExpired = onSessionExpired;
    }

    // ---- session -------------------------------------------------------

    public boolean hasSession() {
        return session != null;
    }

    public String getUserId() {
        return session == null ? null : session.get("user").getString("id");
    }

    public String getUserEmail() {
        return session == null ? null : session.get("user").getString("email");
    }

    /** Keeps the Auth response (tokens + user) so the next visit starts signed in, even offline. */
    public void saveSession(Json authResponse) {
        session = authResponse;
        store.set(SESSION_KEY, authResponse.stringify());
    }

    public void clearSession() {
        session = null;
        store.remove(SESSION_KEY);
    }

    // ---- requests ------------------------------------------------------

    /** Supabase Auth endpoint, e.g. "/auth/v1/token?grant_type=password". */
    public void auth(String method, String path, Json body, boolean withToken,
                     Consumer<Json> onOk, Consumer<RepoError> onError) {
        Map<String, String> headers = baseHeaders();
        if (withToken && session != null) {
            headers.put("Authorization", "Bearer " + session.getString("access_token"));
        }
        http.send(method, url + path, headers, body == null ? null : body.stringify(), response -> {
            if (response.isOk()) {
                String text = response.getBody();
                onOk.accept(text == null || text.isEmpty() ? Json.object() : Json.parse(text));
            } else {
                onError.accept(errorFrom(response));
            }
        });
    }

    /** PostgREST call, e.g. rest("GET", "/rest/v1/tasks?select=*", null, null, ...). */
    public void rest(String method, String path, String body, String prefer,
                     Consumer<String> onOk, Consumer<RepoError> onError) {
        restAttempt(method, path, body, prefer, true, onOk, onError);
    }

    private void restAttempt(String method, String path, String body, String prefer, boolean mayRetry,
                             Consumer<String> onOk, Consumer<RepoError> onError) {
        Map<String, String> headers = baseHeaders();
        if (session != null) {
            headers.put("Authorization", "Bearer " + session.getString("access_token"));
        }
        if (prefer != null) {
            headers.put("Prefer", prefer);
        }
        http.send(method, url + path, headers, body, response -> {
            if (response.isOk()) {
                onOk.accept(response.getBody());
            } else if (response.getStatus() == 401 && mayRetry && session != null) {
                refreshThen(() -> restAttempt(method, path, body, prefer, false, onOk, onError), onError);
            } else {
                onError.accept(errorFrom(response));
            }
        });
    }

    /** Swaps the refresh token for a new access token, then runs the request again. */
    private void refreshThen(Runnable retry, Consumer<RepoError> onError) {
        waitingForRefresh.add(retry);
        if (refreshing) {
            return;
        }
        refreshing = true;
        Json body = Json.object().put("refresh_token", session.getString("refresh_token"));
        auth("POST", "/auth/v1/token?grant_type=refresh_token", body, false, fresh -> {
            refreshing = false;
            saveSession(fresh);
            List<Runnable> retries = new ArrayList<>(waitingForRefresh);
            waitingForRefresh.clear();
            for (Runnable r : retries) {
                r.run();
            }
        }, error -> {
            refreshing = false;
            waitingForRefresh.clear();
            if (!error.isOffline()) {
                clearSession();
                onSessionExpired.run();
            }
            onError.accept(error.isOffline() ? error : RepoError.of("Your session ended. Please sign in again."));
        });
    }

    private Map<String, String> baseHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("apikey", anonKey);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    /** Pulls the readable message out of an Auth or PostgREST error body. */
    private static RepoError errorFrom(HttpClient.Response response) {
        if (response.isNetworkError()) {
            return RepoError.offline();
        }
        String message = "Something went wrong (" + response.getStatus() + "). Try again.";
        String text = response.getBody();
        if (text != null && text.startsWith("{")) {
            Json json = Json.parse(text);
            for (String key : new String[] {"msg", "message", "error_description", "error"}) {
                if (json.has(key)) {
                    message = json.getString(key);
                    break;
                }
            }
        }
        return new RepoError(message, false, response.getStatus());
    }
}
