package app.studybloom;

import app.studybloom.ui.App;
import app.studybloom.web.AppConfig;
import app.studybloom.web.Js;
import org.teavm.jso.dom.html.HTMLDocument;

/** Entry point. TeaVM compiles this (and everything it uses) to dist/js/studybloom.js. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Js.registerServiceWorker();
        AppServices services = new AppServices(AppConfig.fromWindow());
        new App(services, HTMLDocument.current().getElementById("app")).start();
    }
}
