// Generated at build time from environment variables (see .env.example).
// Only PUBLIC values belong here. Never put the Supabase service role key
// or the Stripe secret key in this file.
window.STUDYBLOOM_CONFIG = {
  supabaseUrl: "${SUPABASE_URL}",
  supabaseAnonKey: "${SUPABASE_ANON_KEY}",
  stripeProLink: "${STRIPE_PAYMENT_LINK_PRO}",
  stripeMaxLink: "${STRIPE_PAYMENT_LINK_MAX}",
  buildVersion: "${build.version}"
};
