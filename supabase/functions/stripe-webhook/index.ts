// StudyBloom: Stripe webhook (the ONLY non-Java code in the app's logic).
//
// Stripe calls this after a test-mode payment. It verifies Stripe's signature,
// works out which plan was bought, and updates public.subscriptions with the
// service role key (which bypasses RLS and never leaves the server).
//
// Deploy:  supabase functions deploy stripe-webhook --no-verify-jwt
// Secrets: supabase secrets set STRIPE_SECRET_KEY=sk_test_... STRIPE_WEBHOOK_SECRET=whsec_...
//          STRIPE_PRO_LINK_ID=plink_... STRIPE_MAX_LINK_ID=plink_...
// (SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are provided automatically.)

import Stripe from "npm:stripe@17.7.0";
import { createClient } from "npm:@supabase/supabase-js@2.49.4";

const stripe = new Stripe(Deno.env.get("STRIPE_SECRET_KEY")!);
const cryptoProvider = Stripe.createSubtleCryptoProvider();
const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

// Which Payment Link means which plan.
const PLAN_BY_LINK: Record<string, "pro" | "max"> = {
  [Deno.env.get("STRIPE_PRO_LINK_ID") ?? "unset-pro"]: "pro",
  [Deno.env.get("STRIPE_MAX_LINK_ID") ?? "unset-max"]: "max",
};

// Stripe statuses we store as-is; anything else is treated as canceled.
const KNOWN_STATUS = ["active", "trialing", "past_due", "canceled", "incomplete", "unpaid"];

// Newer Stripe API versions moved current_period_end onto the subscription item.
function periodEnd(sub: Stripe.Subscription): string | null {
  // deno-lint-ignore no-explicit-any
  const seconds = (sub as any).current_period_end ?? (sub.items?.data?.[0] as any)?.current_period_end;
  return seconds ? new Date(seconds * 1000).toISOString() : null;
}

Deno.serve(async (req) => {
  // 1. Verify the request really comes from Stripe.
  const signature = req.headers.get("Stripe-Signature");
  const body = await req.text();
  let event: Stripe.Event;
  try {
    event = await stripe.webhooks.constructEventAsync(
      body, signature!, Deno.env.get("STRIPE_WEBHOOK_SECRET")!, undefined, cryptoProvider,
    );
  } catch (err) {
    return new Response(`Bad signature: ${(err as Error).message}`, { status: 400 });
  }

  try {
    // 2. A checkout finished: the user id comes from client_reference_id,
    //    set by StripeCheckout.java when it opened the Payment Link.
    if (event.type === "checkout.session.completed") {
      const session = event.data.object as Stripe.Checkout.Session;
      const userId = session.client_reference_id;
      const linkId = typeof session.payment_link === "string" ? session.payment_link : session.payment_link?.id;
      const plan = (linkId && PLAN_BY_LINK[linkId]) ?? (session.metadata?.plan as "pro" | "max" | undefined);
      if (!userId || !plan) {
        return new Response("Missing user or plan; ignored", { status: 200 });
      }
      const subscriptionId = typeof session.subscription === "string" ? session.subscription : session.subscription?.id;
      const sub = subscriptionId ? await stripe.subscriptions.retrieve(subscriptionId) : null;

      const { error } = await supabase.from("subscriptions").upsert({
        user_id: userId,
        plan,
        status: sub?.status ?? "active",
        current_period_end: sub ? periodEnd(sub) : null,
        stripe_customer_id: typeof session.customer === "string" ? session.customer : session.customer?.id,
        stripe_subscription_id: subscriptionId ?? null,
        is_demo: false,
        updated_at: new Date().toISOString(),
      });
      if (error) throw error;
    }

    // 3. Renewals, failed payments and cancellations keep the row in sync.
    if (event.type === "customer.subscription.updated" || event.type === "customer.subscription.deleted") {
      const sub = event.data.object as Stripe.Subscription;
      const ended = event.type === "customer.subscription.deleted";
      const status = ended ? "canceled" : (KNOWN_STATUS.includes(sub.status) ? sub.status : "canceled");
      const update: Record<string, unknown> = {
        status,
        current_period_end: periodEnd(sub),
        updated_at: new Date().toISOString(),
      };
      if (ended) update.plan = "free";
      const { error } = await supabase.from("subscriptions").update(update).eq("stripe_subscription_id", sub.id);
      if (error) throw error;
    }

    return new Response(JSON.stringify({ received: true }), {
      headers: { "Content-Type": "application/json" },
    });
  } catch (err) {
    // 500 makes Stripe retry later.
    console.error(err);
    return new Response("Webhook handler failed", { status: 500 });
  }
});
