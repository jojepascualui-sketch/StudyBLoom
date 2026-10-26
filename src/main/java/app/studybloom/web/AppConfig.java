package app.studybloom.web;

/**
 * Public settings baked into config.js at build time from environment
 * variables. Only public values live here: the Supabase URL, the anon key and
 * the Stripe payment link URLs. Secret keys never reach the browser.
 */
public class AppConfig {
    private final String supabaseUrl;
    private final String supabaseAnonKey;
    private final String proPaymentLink;
    private final String maxPaymentLink;

    public AppConfig(String supabaseUrl, String supabaseAnonKey, String proPaymentLink, String maxPaymentLink) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
        this.proPaymentLink = proPaymentLink;
        this.maxPaymentLink = maxPaymentLink;
    }

    public static AppConfig fromWindow() {
        return new AppConfig(Js.config("supabaseUrl"), Js.config("supabaseAnonKey"),
                Js.config("stripeProLink"), Js.config("stripeMaxLink"));
    }

    /** False means "local demo mode": everything is stored on this device. */
    public boolean isSupabaseConfigured() {
        return supabaseUrl.startsWith("https://") && !supabaseAnonKey.isEmpty();
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getSupabaseAnonKey() {
        return supabaseAnonKey;
    }

    public String getProPaymentLink() {
        return proPaymentLink;
    }

    public String getMaxPaymentLink() {
        return maxPaymentLink;
    }
}
