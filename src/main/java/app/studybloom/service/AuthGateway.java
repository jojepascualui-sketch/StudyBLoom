package app.studybloom.service;

import app.studybloom.model.Student;
import app.studybloom.repo.RepoError;
import java.util.function.Consumer;

/**
 * ABSTRACTION over the login backend. SupabaseAuthGateway calls Supabase Auth;
 * LocalAuthGateway keeps a pretend account on the device for local demo mode.
 */
public interface AuthGateway {
    void signIn(String email, String password, Consumer<Student> onOk, Consumer<RepoError> onError);

    /** onOk receives null when the student must confirm their email before signing in. */
    void signUp(String email, String password, Consumer<Student> onOk, Consumer<RepoError> onError);

    void sendPasswordReset(String email, Runnable onOk, Consumer<RepoError> onError);

    /** The student saved from a previous visit, or null. Works offline. */
    Student restoreSession();

    void signOut(Runnable onDone);
}
