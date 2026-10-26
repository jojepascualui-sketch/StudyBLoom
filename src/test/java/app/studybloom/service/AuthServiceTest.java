package app.studybloom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.studybloom.model.Student;
import app.studybloom.repo.RepoError;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    /** Fake backend: returns a fixed error, or signs in. */
    private static class FakeGateway implements AuthGateway {
        RepoError error;
        int calls;

        @Override
        public void signIn(String email, String password, Consumer<Student> onOk, Consumer<RepoError> onError) {
            calls++;
            if (error != null) {
                onError.accept(error);
            } else {
                onOk.accept(new Student("id-1", email));
            }
        }

        @Override
        public void signUp(String email, String password, Consumer<Student> onOk, Consumer<RepoError> onError) {
            signIn(email, password, onOk, onError);
        }

        @Override
        public void sendPasswordReset(String email, Runnable onOk, Consumer<RepoError> onError) {
            onOk.run();
        }

        @Override
        public Student restoreSession() {
            return null;
        }

        @Override
        public void signOut(Runnable onDone) {
            onDone.run();
        }
    }

    private final FakeGateway gateway = new FakeGateway();
    private final AuthService auth = new AuthService(gateway);

    private ValidationError signInError(String email, String password) {
        AtomicReference<ValidationError> error = new AtomicReference<>();
        auth.signIn(email, password, s -> { }, error::set);
        return error.get();
    }

    @Test
    void checksTheFormBeforeCallingTheServer() {
        assertEquals("email", signInError("", "secret1").getField());
        assertEquals("email", signInError("bea@school", "secret1").getField());
        assertEquals("password", signInError("bea@school.edu.ph", "123").getField());
        assertEquals(0, gateway.calls);
    }

    @Test
    void signsInAndRemembersTheStudent() {
        assertNull(signInError(" bea.santos@school.edu.ph ", "secret1"));
        assertTrue(auth.isSignedIn());
        assertEquals("bea.santos@school.edu.ph", auth.getCurrentStudent().getEmail());
        assertEquals("Bea", auth.getCurrentStudent().getFirstName());
    }

    @Test
    void wrongPasswordShowsUnderThePasswordField() {
        gateway.error = new RepoError("Invalid login credentials", false, 400);
        ValidationError error = signInError("bea@school.edu.ph", "secret1");
        assertEquals("password", error.getField());
        assertTrue(error.getMessage().contains("Forgot password"));
    }

    @Test
    void offlineIsAFormLevelMessage() {
        gateway.error = RepoError.offline();
        ValidationError error = signInError("bea@school.edu.ph", "secret1");
        assertEquals(ValidationError.FORM, error.getField());
        assertTrue(error.getMessage().contains("offline"));
    }

    @Test
    void existingAccountOnSignUp() {
        gateway.error = new RepoError("User already registered", false, 422);
        AtomicReference<ValidationError> error = new AtomicReference<>();
        auth.signUp("bea@school.edu.ph", "secret1", s -> { }, error::set);
        assertEquals("email", error.get().getField());
    }

    @Test
    void signOutForgetsTheStudent() {
        signInError("bea@school.edu.ph", "secret1");
        auth.signOut(() -> { });
        assertNull(auth.getCurrentStudent());
    }

    @Test
    void passwordResetNeedsAnEmail() {
        AtomicReference<ValidationError> error = new AtomicReference<>();
        auth.sendPasswordReset("", () -> { }, error::set);
        assertNotNull(error.get());
    }
}
