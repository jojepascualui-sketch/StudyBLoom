package app.studybloom.web;

import app.studybloom.model.Student;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.RepoError;
import app.studybloom.service.AuthGateway;
import java.util.function.Consumer;

/**
 * Local demo mode sign-in, used only when no Supabase project is configured.
 * It is NOT secure (one account per browser, password hash in localStorage);
 * it exists so the app can be tried before Supabase is set up.
 */
public class LocalAuthGateway implements AuthGateway {
    private static final String ACCOUNT = "sb.local.account";
    private static final String SESSION = "sb.local.session";

    private final KeyValueStore store;

    public LocalAuthGateway(KeyValueStore store) {
        this.store = store;
    }

    @Override
    public void signIn(String email, String password, Consumer<Student> onOk, Consumer<RepoError> onError) {
        String saved = store.get(ACCOUNT);
        if (saved == null) {
            // First visit: signing in creates the local account.
            signUp(email, password, onOk, onError);
            return;
        }
        Json account = Json.parse(saved);
        if (!account.getString("email").equalsIgnoreCase(email) || !hash(password).equals(account.getString("hash"))) {
            onError.accept(RepoError.of("Invalid login credentials"));
            return;
        }
        store.set(SESSION, email);
        onOk.accept(student(email));
    }

    @Override
    public void signUp(String email, String password, Consumer<Student> onOk, Consumer<RepoError> onError) {
        store.set(ACCOUNT, Json.object().put("email", email).put("hash", hash(password)).stringify());
        store.set(SESSION, email);
        onOk.accept(student(email));
    }

    @Override
    public void sendPasswordReset(String email, Runnable onOk, Consumer<RepoError> onError) {
        onError.accept(RepoError.of("Password reset needs Supabase. In local mode, create a new account instead."));
    }

    @Override
    public Student restoreSession() {
        String email = store.get(SESSION);
        return email == null ? null : student(email);
    }

    @Override
    public void signOut(Runnable onDone) {
        store.remove(SESSION);
        onDone.run();
    }

    private static Student student(String email) {
        return new Student("local-" + Integer.toHexString(email.toLowerCase().hashCode()), email);
    }

    private static String hash(String password) {
        return Integer.toHexString(("studybloom:" + password).hashCode());
    }
}
