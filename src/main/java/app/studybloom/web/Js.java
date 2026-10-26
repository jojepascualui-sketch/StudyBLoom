package app.studybloom.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * The small TeaVM interop layer: each method is one browser call that the
 * TeaVM JSO API does not cover. Everything else in the app is plain Java.
 */
public final class Js {
    private Js() {
    }

    @JSFunctor
    public interface Callback extends JSObject {
        void call();
    }

    @JSFunctor
    public interface StringCallback extends JSObject {
        void call(String value);
    }

    // ---- time ----------------------------------------------------------

    @JSBody(script = "return Date.now();")
    public static native double now();

    @JSBody(params = "ms", script = "return -new Date(ms).getTimezoneOffset();")
    public static native int offsetMinutes(double ms);

    @JSBody(params = "iso", script = "if (!iso) return 0; var t = Date.parse(iso); return isNaN(t) ? 0 : t;")
    public static native double parseIso(String iso);

    @JSBody(params = "ms", script = "return new Date(ms).toISOString();")
    public static native String toIso(double ms);

    // ---- ids, config, navigation ---------------------------------------

    @JSBody(script = "if (self.crypto && crypto.randomUUID) return crypto.randomUUID();"
            + "return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {"
            + "var r = Math.random() * 16 | 0; return (c === 'x' ? r : (r & 3 | 8)).toString(16); });")
    public static native String uuid();

    /** Reads window.STUDYBLOOM_CONFIG, written into config.js at build time. */
    @JSBody(params = "key", script = "var c = window.STUDYBLOOM_CONFIG || {}; var v = c[key];"
            + "return (typeof v === 'string' && v.indexOf('${') !== 0) ? v.trim() : '';")
    public static native String config(String key);

    @JSBody(params = "url", script = "window.location.href = url;")
    public static native void redirect(String url);

    @JSBody(params = "name", script = "return new URLSearchParams(window.location.search).get(name) || '';")
    public static native String queryParam(String name);

    @JSBody(script = "window.history.replaceState(null, '', window.location.pathname);")
    public static native void clearQuery();

    @JSBody(params = "message", script = "console.warn('[StudyBloom] ' + message);")
    public static native void warn(String message);

    // ---- connectivity and events ---------------------------------------

    @JSBody(script = "return navigator.onLine !== false;")
    public static native boolean isOnline();

    @JSBody(params = {"name", "cb"}, script = "window.addEventListener(name, function () { cb(); });")
    public static native void onWindowEvent(String name, Callback cb);

    @JSBody(params = "cb", script = "document.addEventListener('visibilitychange', function () {"
            + "if (document.visibilityState === 'visible') cb(); });")
    public static native void onVisible(Callback cb);

    // ---- theme and motion ----------------------------------------------

    @JSBody(script = "return !!(window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches);")
    public static native boolean prefersReducedMotion();

    @JSBody(script = "return !!(window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches);")
    public static native boolean prefersDark();

    @JSBody(params = "cb", script = "if (!window.matchMedia) return; var m = matchMedia('(prefers-color-scheme: dark)');"
            + "if (m.addEventListener) m.addEventListener('change', function () { cb(); });")
    public static native void onSystemThemeChange(Callback cb);

    @JSBody(params = "dark", script = "document.documentElement.classList.toggle('dark', dark);"
            + "var m = document.querySelector('meta[name=theme-color]');"
            + "if (m) m.setAttribute('content', dark ? '#2A1024' : '#F472B6');")
    public static native void setDarkClass(boolean dark);

    // ---- install (PWA) -------------------------------------------------

    @JSBody(script = "return /iphone|ipad|ipod/i.test(navigator.userAgent)"
            + " || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);")
    public static native boolean isIos();

    @JSBody(script = "return !!((window.matchMedia && matchMedia('(display-mode: standalone)').matches)"
            + " || navigator.standalone === true);")
    public static native boolean isStandalone();

    /** index.html stores the beforeinstallprompt event in window.__sbInstallPrompt. */
    @JSBody(script = "return !!window.__sbInstallPrompt;")
    public static native boolean hasInstallPrompt();

    @JSBody(params = "cb", script = "window.addEventListener('sb-install-available', function () { cb(); });"
            + "window.addEventListener('appinstalled', function () { window.__sbInstallPrompt = null; cb(); });")
    public static native void onInstallStateChange(Callback cb);

    @JSBody(params = "cb", script = "var p = window.__sbInstallPrompt; if (!p) { cb('unavailable'); return; }"
            + "p.prompt(); p.userChoice.then(function (r) { window.__sbInstallPrompt = null; cb(r.outcome); },"
            + "function () { cb('dismissed'); });")
    public static native void promptInstall(StringCallback cb);

    @JSBody(script = "if ('serviceWorker' in navigator) {"
            + "navigator.serviceWorker.register('/sw.js').catch(function (e) { console.warn('SW failed', e); }); }")
    public static native void registerServiceWorker();

    // ---- notifications -------------------------------------------------

    @JSBody(script = "return ('Notification' in window) ? Notification.permission : 'unsupported';")
    public static native String notificationPermission();

    @JSBody(params = "cb", script = "if (!('Notification' in window)) { cb('unsupported'); return; }"
            + "Notification.requestPermission().then(function (p) { cb(p); });")
    public static native void requestNotificationPermission(StringCallback cb);

    @JSBody(params = {"title", "body", "tag"}, script = "var opts = { body: body, tag: tag,"
            + " icon: '/icons/icon-192.png', badge: '/icons/icon-192.png' };"
            + "if ('serviceWorker' in navigator) { navigator.serviceWorker.ready.then(function (r) {"
            + " r.showNotification(title, opts); }); } else { new Notification(title, opts); }")
    public static native void showNotification(String title, String body, String tag);
}
