package app.studybloom.web;

import app.studybloom.service.Notifier;

/** Shows reminders through the service worker's showNotification. */
public class BrowserNotifier implements Notifier {
    @Override
    public boolean isPermitted() {
        return "granted".equals(Js.notificationPermission());
    }

    @Override
    public void notify(String title, String body, String tag) {
        Js.showNotification(title, body, tag);
    }
}
