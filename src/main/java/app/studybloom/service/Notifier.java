package app.studybloom.service;

/** Shows a system notification. In the browser it goes through the service worker. */
public interface Notifier {
    boolean isPermitted();

    void notify(String title, String body, String tag);
}
