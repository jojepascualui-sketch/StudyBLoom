package app.studybloom.ui.screens;

import app.studybloom.model.FreePlan;
import app.studybloom.service.AuthService;
import app.studybloom.service.ValidationError;
import app.studybloom.ui.App;
import app.studybloom.ui.Dom;
import app.studybloom.ui.Icons;
import app.studybloom.ui.NavTab;
import app.studybloom.ui.Screen;
import app.studybloom.ui.components.FormField;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/** Screen 1: Sign in (and Create an account) with Supabase email and password. */
public class LoginScreen extends Screen {
    private boolean signUpMode;
    private FormField emailField;
    private FormField passwordField;
    private HTMLInputElement email;
    private HTMLInputElement password;
    private HTMLElement formError;
    private HTMLElement submit;
    private String rememberedEmail = "";

    public LoginScreen(App app) {
        super(app);
    }

    @Override
    public NavTab getTab() {
        return null;
    }

    @Override
    public String getTitle() {
        return signUpMode ? "Create an account" : "Sign in";
    }

    @Override
    protected HTMLElement build() {
        HTMLElement page = Dom.div("login");

        HTMLElement brand = Dom.div("login-brand");
        Dom.append(brand, Dom.svg("div", "login-logo", Icons.logo(96)),
                Dom.text("h1", "h-title login-title", "StudyBloom"),
                Dom.text("p", "h-sub", "Every assignment in one place, so nothing sneaks up on you."));

        HTMLElement form = Dom.el("form", "login-form");
        form.setAttribute("novalidate", "");
        form.addEventListener("submit", event -> {
            event.preventDefault();
            submit();
        });

        email = Dom.input("email", "input");
        email.setAttribute("autocomplete", "email");
        email.setAttribute("inputmode", "email");
        email.setPlaceholder("name@school.edu.ph");
        email.setValue(rememberedEmail);
        emailField = new FormField("Email", email);

        password = Dom.input("password", "input");
        password.setAttribute("autocomplete", signUpMode ? "new-password" : "current-password");
        password.setPlaceholder(signUpMode ? "At least " + AuthService.MIN_PASSWORD_LENGTH + " characters" : "");
        passwordField = new FormField("Password", password);

        formError = Dom.el("p", "form-error");
        formError.setAttribute("role", "alert");
        formError.setHidden(true);

        submit = Dom.el("button", "btn btn-primary");
        submit.setAttribute("type", "submit");
        submit.setTextContent(signUpMode ? "Create account" : "Sign in");

        Dom.append(form, emailField.getElement(), passwordField.getElement());
        if (!signUpMode) {
            HTMLElement forgotRow = Dom.div("forgot-row");
            forgotRow.appendChild(Dom.button("link", "Forgot password?", this::forgot));
            form.appendChild(forgotRow);
        }
        Dom.append(form, formError, submit);

        HTMLElement switchMode = Dom.button("btn btn-ghost", signUpMode ? "I already have an account" : "Create an account", () -> {
            rememberedEmail = email.getValue();
            signUpMode = !signUpMode;
            app().rerender();
        });

        HTMLElement footer = Dom.div("login-footer");
        if (app().services().isLocalMode()) {
            footer.appendChild(Dom.text("p", "local-note",
                    "Local demo mode: Supabase isn't configured, so your tasks stay on this device."));
        }
        footer.appendChild(Dom.text("p", "", "Free forever for up to " + FreePlan.FREE_TASK_LIMIT + " active tasks"));

        Dom.append(page, brand, form, Dom.div("spacer-10"), switchMode, footer);
        return page;
    }

    @Override
    public void onShown() {
        if (email.getValue().isEmpty()) {
            email.focus();
        }
    }

    private void submit() {
        clearErrors();
        setBusy(true);
        AuthService auth = app().services().getAuth();
        if (signUpMode) {
            auth.signUp(email.getValue(), password.getValue(), student -> {
                setBusy(false);
                if (student == null) {
                    signUpMode = false;
                    rememberedEmail = email.getValue();
                    app().rerender();
                    emailField.setInfo("Almost there! Open the link we emailed you, then sign in here.");
                } else {
                    app().enterApp();
                }
            }, this::showError);
        } else {
            auth.signIn(email.getValue(), password.getValue(), student -> {
                setBusy(false);
                app().enterApp();
            }, this::showError);
        }
    }

    private void forgot() {
        clearErrors();
        app().services().getAuth().sendPasswordReset(email.getValue(),
                () -> emailField.setInfo("Reset link sent. Check your inbox, then come back to sign in."),
                this::showError);
    }

    private void showError(ValidationError error) {
        setBusy(false);
        switch (error.getField()) {
            case "email":
                emailField.setError(error.getMessage());
                email.focus();
                break;
            case "password":
                passwordField.setError(error.getMessage());
                password.focus();
                break;
            default:
                formError.setTextContent(error.getMessage());
                formError.setHidden(false);
        }
    }

    private void clearErrors() {
        emailField.clearError();
        passwordField.clearError();
        formError.setHidden(true);
    }

    private void setBusy(boolean busy) {
        if (busy) {
            submit.setAttribute("disabled", "");
            submit.setTextContent(signUpMode ? "Creating account…" : "Signing in…");
        } else {
            submit.removeAttribute("disabled");
            submit.setTextContent(signUpMode ? "Create account" : "Sign in");
        }
    }
}
