package app.studybloom.web;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.json.JSON;

/**
 * ENCAPSULATION: wraps a raw JavaScript object so the rest of the Java code
 * reads and writes JSON with typed methods instead of touching JS directly.
 */
public final class Json {
    private final JSObject raw;

    private Json(JSObject raw) {
        this.raw = raw;
    }

    public static Json parse(String text) {
        return new Json(JSON.parse(text == null || text.isEmpty() ? "null" : text));
    }

    public static Json object() {
        return new Json(newObject());
    }

    public static Json array() {
        return new Json(newArray());
    }

    public String stringify() {
        return JSON.stringify(raw);
    }

    // ---- objects -------------------------------------------------------

    public boolean isNull() {
        return isNullJs(raw);
    }

    public boolean has(String key) {
        return hasJs(raw, key);
    }

    public String getString(String key) {
        return hasJs(raw, key) ? stringJs(raw, key) : null;
    }

    public boolean getBoolean(String key) {
        return boolJs(raw, key);
    }

    public double getNumber(String key) {
        return numberJs(raw, key);
    }

    /** Reads an ISO timestamp as epoch millis, 0 when missing. */
    public long getTimestamp(String key) {
        return (long) Js.parseIso(getString(key));
    }

    public Json get(String key) {
        return new Json(getJs(raw, key));
    }

    public Json put(String key, String value) {
        if (value == null) {
            putNullJs(raw, key);
        } else {
            putStringJs(raw, key, value);
        }
        return this;
    }

    public Json put(String key, boolean value) {
        putBoolJs(raw, key, value);
        return this;
    }

    public Json put(String key, double value) {
        putNumberJs(raw, key, value);
        return this;
    }

    public Json put(String key, Json value) {
        putObjectJs(raw, key, value.raw);
        return this;
    }

    /** Writes epoch millis as an ISO timestamp, or null when 0. */
    public Json putTimestamp(String key, long millis) {
        return put(key, millis <= 0 ? null : Js.toIso(millis));
    }

    // ---- arrays --------------------------------------------------------

    public boolean isArray() {
        return isArrayJs(raw);
    }

    public int size() {
        return isArrayJs(raw) ? lengthJs(raw) : 0;
    }

    public Json at(int index) {
        return new Json(atJs(raw, index));
    }

    public Json add(Json value) {
        pushJs(raw, value.raw);
        return this;
    }

    public List<Json> items() {
        List<Json> list = new ArrayList<>();
        for (int i = 0; i < size(); i++) {
            list.add(at(i));
        }
        return list;
    }

    // ---- interop -------------------------------------------------------

    @JSBody(script = "return {};")
    private static native JSObject newObject();

    @JSBody(script = "return [];")
    private static native JSObject newArray();

    @JSBody(params = "o", script = "return o === null || o === undefined;")
    private static native boolean isNullJs(JSObject o);

    @JSBody(params = {"o", "k"}, script = "return o !== null && o !== undefined && o[k] !== undefined && o[k] !== null;")
    private static native boolean hasJs(JSObject o, String k);

    @JSBody(params = {"o", "k"}, script = "return String(o[k]);")
    private static native String stringJs(JSObject o, String k);

    @JSBody(params = {"o", "k"}, script = "return !!(o && o[k]);")
    private static native boolean boolJs(JSObject o, String k);

    @JSBody(params = {"o", "k"}, script = "var v = o ? o[k] : null; v = Number(v); return isNaN(v) ? 0 : v;")
    private static native double numberJs(JSObject o, String k);

    @JSBody(params = {"o", "k"}, script = "return o ? (o[k] === undefined ? null : o[k]) : null;")
    private static native JSObject getJs(JSObject o, String k);

    @JSBody(params = {"o", "k", "v"}, script = "o[k] = v;")
    private static native void putStringJs(JSObject o, String k, String v);

    @JSBody(params = {"o", "k", "v"}, script = "o[k] = v;")
    private static native void putBoolJs(JSObject o, String k, boolean v);

    @JSBody(params = {"o", "k", "v"}, script = "o[k] = v;")
    private static native void putNumberJs(JSObject o, String k, double v);

    @JSBody(params = {"o", "k", "v"}, script = "o[k] = v;")
    private static native void putObjectJs(JSObject o, String k, JSObject v);

    @JSBody(params = {"o", "k"}, script = "o[k] = null;")
    private static native void putNullJs(JSObject o, String k);

    @JSBody(params = "o", script = "return Array.isArray(o);")
    private static native boolean isArrayJs(JSObject o);

    @JSBody(params = "o", script = "return o.length;")
    private static native int lengthJs(JSObject o);

    @JSBody(params = {"o", "i"}, script = "return o[i];")
    private static native JSObject atJs(JSObject o, int i);

    @JSBody(params = {"o", "v"}, script = "o.push(v);")
    private static native void pushJs(JSObject o, JSObject v);
}
