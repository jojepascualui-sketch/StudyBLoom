package app.studybloom.web;

import app.studybloom.model.Student;
import app.studybloom.repo.RepoError;
import app.studybloom.service.AuthGateway;
import java.util.function.Consumer;

/** POLYMORPHISM: the AuthGateway used when Supabase is configured. */
public class SupabaseAuthGateway implements AuthGateway {
    private final SupabaseClient client;

    public SupabaseAuthGateway(SupabaseClient client) {
        this.client = client;
    }

    @Override
    public void signIn(String email, String password, Consumer<Student> onOk, Consumer<RepoError> onError) {
        Json body = Json.object().put("email", email).put("password", password);
        client.auth("POST", "/auth/v1/token?grant_type=password", body, false, response -> {
            client.saveSession(response);
            onOk.accept(currentStudent());
        }, onError);
    }

    @Override
    public void signUp(String email, String password, Consumer<Student> onOk, Consumer<RepoError> onError) {
        Json body = Json.object().put("email", email).put("password", password);
        client.auth("POST", "/auth/v1/signup", body, false, response -> {
            if (response.has("access_token")) {
                client.saveSession(response);
                onOk.accept(currentStudent());
            } else {
                // Email confirmation is on in Supabase: no session until the link is clicked.
                onOk.accept(null);
            }
        }, onError);
    }

    @Override
    public void sendPasswordReset(String email, Runnable onOk, Consumer<RepoError> onError) {
        client.auth("POST", "/auth/v1/recover", Json.object().put("email", email), false, ok -> onOk.run(), onError);
    }

    @Override
    public Student restoreSession() {
        return client.hasSession() ? currentStudent() : null;
    }

    @Override
    public void signOut(Runnable onDone) {
        client.auth("POST", "/auth/v1/logout", null, true, ok -> {
            client.clearSession();
            onDone.run();
        }, error -> {
            client.clearSession();
            onDone.run();
        });
    }

    private Student currentStudent() {
        return new Student(client.getUserId(), client.getUserEmail());
    }
}
