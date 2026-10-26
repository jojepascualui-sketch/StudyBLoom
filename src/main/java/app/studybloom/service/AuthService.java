package app.studybloom.service;

import app.studybloom.model.Student;
import app.studybloom.repo.RepoError;
import java.util.function.Consumer;

/**
 * Sign in, sign up, sign out. Checks the form first, then turns backend errors
 * into messages that say how to fix the problem.
 */
public class AuthService {
    public static final int MIN_PASSWORD_LENGTH = 6;

    private final AuthGateway gateway;
    private Student current;

    public AuthService(AuthGateway gateway) {
        this.gateway = gateway;
    }

    public Student getCurrentStudent() {
        return current;
    }

    public boolean isSignedIn() {
        return current != null;
    }

    /** Picks up a session saved in an earlier visit. */
    public boolean restore() {
        current = gateway.restoreSession();
        return current != null;
    }

    public ValidationError validateEmail(String email) {
        String value = email == null ? "" : email.trim();
        if (value.isEmpty()) {
            return new ValidationError("email", "Enter your email, like name@school.edu.ph.");
        }
        int at = value.indexOf('@');
        if (at < 1 || value.indexOf('.', at) < at + 2 || value.endsWith(".") || value.contains(" ")) {
            return new ValidationError("email", "That email looks incomplete. Check the part after the @.");
        }
        return null;
    }

    public ValidationError validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationError("password", "Enter your password.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new ValidationError("password", "Use at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        return null;
    }

    public void signIn(String email, String password, Consumer<Student> onOk, Consumer<ValidationError> onError) {
        ValidationError problem = firstProblem(email, password);
        if (problem != null) {
            onError.accept(problem);
            return;
        }
        gateway.signIn(email.trim(), password, student -> {
            current = student;
            onOk.accept(student);
        }, error -> onError.accept(translate(error)));
    }

    /** onOk gets null when Supabase wants the email confirmed first. */
    public void signUp(String email, String password, Consumer<Student> onOk, Consumer<ValidationError> onError) {
        ValidationError problem = firstProblem(email, password);
        if (problem != null) {
            onError.accept(problem);
            return;
        }
        gateway.signUp(email.trim(), password, student -> {
            current = student;
            onOk.accept(student);
        }, error -> onError.accept(translate(error)));
    }

    public void sendPasswordReset(String email, Runnable onOk, Consumer<ValidationError> onError) {
        ValidationError problem = validateEmail(email);
        if (problem != null) {
            onError.accept(new ValidationError("email", "Enter your email above first, then tap Forgot password."));
            return;
        }
        gateway.sendPasswordReset(email.trim(), onOk, error -> onError.accept(translate(error)));
    }

    public void signOut(Runnable onDone) {
        current = null;
        gateway.signOut(onDone);
    }

    private ValidationError firstProblem(String email, String password) {
        ValidationError problem = validateEmail(email);
        return problem != null ? problem : validatePassword(password);
    }

    /** Maps Supabase Auth messages to friendly, fixable ones. */
    ValidationError translate(RepoError error) {
        if (error.isOffline()) {
            return new ValidationError(ValidationError.FORM, "You're offline. Connect to the internet to sign in.");
        }
        String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase();
        if (message.contains("invalid login")) {
            return new ValidationError("password",
                    "That email and password don't match. Check both, or tap Forgot password.");
        }
        if (message.contains("not confirmed")) {
            return new ValidationError("email", "Confirm your email first. Open the link we sent to your inbox.");
        }
        if (message.contains("already registered") || message.contains("already exists")) {
            return new ValidationError("email", "That email already has an account. Sign in instead.");
        }
        if (message.contains("password")) {
            return new ValidationError("password", error.getMessage());
        }
        if (message.contains("rate limit") || error.getStatus() == 429) {
            return new ValidationError(ValidationError.FORM, "Too many tries. Wait a minute, then try again.");
        }
        return new ValidationError(ValidationError.FORM, error.getMessage());
    }
}
