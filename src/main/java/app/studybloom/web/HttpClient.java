package app.studybloom.web;

import java.util.Map;
import java.util.function.Consumer;
import org.teavm.jso.ajax.XMLHttpRequest;

/** Sends one HTTP request with XMLHttpRequest. Status 0 means no network. */
public class HttpClient {

    public static class Response {
        private final int status;
        private final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }

        public boolean isOk() {
            return status >= 200 && status < 300;
        }

        public boolean isNetworkError() {
            return status == 0;
        }
    }

    public void send(String method, String url, Map<String, String> headers, String body, Consumer<Response> onDone) {
        XMLHttpRequest xhr = new XMLHttpRequest();
        xhr.open(method, url);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            xhr.setRequestHeader(header.getKey(), header.getValue());
        }
        xhr.setOnReadyStateChange(() -> {
            if (xhr.getReadyState() == XMLHttpRequest.DONE) {
                onDone.accept(new Response(xhr.getStatus(), xhr.getResponseText()));
            }
        });
        if (body == null) {
            xhr.send();
        } else {
            xhr.send(body);
        }
    }
}
