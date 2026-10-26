package app.studybloom.repo;

/** Why a repository call failed. {@link #isOffline()} lets callers fall back to cached data. */
public class RepoError {
    private final String message;
    private final boolean offline;
    private final int status;

    public RepoError(String message, boolean offline, int status) {
        this.message = message;
        this.offline = offline;
        this.status = status;
    }

    public static RepoError offline() {
        return new RepoError("You're offline. Changes will sync when you reconnect.", true, 0);
    }

    public static RepoError of(String message) {
        return new RepoError(message, false, 0);
    }

    public String getMessage() {
        return message;
    }

    public boolean isOffline() {
        return offline;
    }

    /** HTTP status, or 0 when there was no response. */
    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return message;
    }
}
