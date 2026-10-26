# StudyBloom

A Progressive Web App that helps one student keep track of school assignments and deadlines.
Written in **Java 21** (object-oriented, no JS frameworks), compiled to JavaScript with **TeaVM**,
backed by **Supabase** (Postgres + Auth), hosted on **Vercel**, with **Stripe** test-mode payments.

The UI follows `design/studybloom-design-reference.html` (colors, fonts, spacing, the six screens).

---

## 1. Project layout

```
pom.xml                         Maven build: resources -> TeaVM -> dist/
mvnw, mvnw.cmd, .mvn/           Maven wrapper (no Maven install needed)
.env.example                    public frontend config (copy to .env)
vercel.json                     Vercel: build command, output dir, headers
scripts/vercel-build.sh         Vercel build (downloads JDK 21, runs Maven)
tools/IconGenerator.java        draws the flower PWA icons (already generated)
design/                         the design reference (source of truth for the UI)

src/main/java/app/studybloom/
  Main.java                     entry point
  AppServices.java              builds all services (Supabase or local mode)
  model/                        Task, Student, Subscription (abstract), FreePlan, ProPlan, MaxPlan,
                                PlanType, Feature, ScheduleEntry, SubjectNote
  time/                         AppClock, LocalDay, DateTexts, TaskGroup (TeaVM has no java.time)
  repo/                         TaskRepository and other interfaces, RepoError, KeyValueStore
  service/                      TaskService, AuthService, SubscriptionService, BloomMeterService,
                                ReminderService, MockCheckout, CheckoutGateway, ...
  web/                          TeaVM interop: Js, Json, HttpClient, SupabaseClient,
                                SupabaseTaskRepository, OfflineTaskRepository, StripeCheckout, ...
  ui/                           App, Screen, Component, Dom, Icons, NavTab
  ui/components/                TaskCard, BloomMeter, PlanCard, Toast, BottomSheet, Toggle, ...
  ui/screens/                   LoginScreen, TaskListScreen, AddTaskScreen, PricingScreen,
                                SettingsScreen, ScheduleScreen
src/main/webapp/                index.html, styles.css, manifest.json, sw.js, config.js, icons/
src/test/java/                  JUnit 5 tests for services and plan rules (46 tests)

supabase/migrations/            SQL: profiles, subscriptions, tasks, schedules + notes, with RLS
supabase/functions/stripe-webhook/index.ts   the one non-Java file of app logic
```

Non-Java files and why they exist: `sw.js` (a service worker must be a JS file the browser
loads directly), the small inline script in `index.html` (applies dark mode before first paint
and catches `beforeinstallprompt`, which can fire before the Java app has loaded), `config.js`
(build-time public config), and the Stripe webhook Edge Function (the brief's one allowed exception).

## 2. Setup

You need **JDK 21** (for example [Temurin 21](https://adoptium.net/temurin/releases/?version=21)).
Maven is not required; use the wrapper.

```bash
cp .env.example .env        # fill in later; leave empty for local demo mode
./mvnw package              # Windows: mvnw.cmd package
```

This runs the unit tests and writes the finished site to `dist/`.

**Local demo mode:** if `SUPABASE_URL` is empty, the app runs entirely in the browser
(tasks in `localStorage`, any email + 6-character password signs in, plan changes are free).
Good for trying the UI before Supabase is set up. The Me screen and sign-in page say when it's on.

## 3. Supabase setup

1. Create a project at [supabase.com](https://supabase.com).
2. **SQL Editor**: run the four files in `supabase/migrations/` in order (or `supabase db push`
   with the Supabase CLI). They create the tables, RLS policies, the plan-limit trigger,
   the sign-up trigger and the demo-mode function.
3. **Authentication > Providers > Email**: enabled (default). For a class demo you can turn off
   "Confirm email" so new accounts can sign in immediately.
4. **Authentication > URL Configuration**: set Site URL to your Vercel URL
   (and add `http://localhost:5173` for local testing).
5. **Project Settings > API**: copy the Project URL and the `anon` `public` key into `.env`.

Security model:
- Row Level Security is on for every table; each student only sees their own rows.
- Students can **read** their subscription but never write it. Plans change only through the
  Stripe webhook (service role key, server-side) or `demo_set_plan()` while demo mode is on.
- The 15-task Free limit is enforced in Java (`SubscriptionService`) **and** in Postgres
  (`enforce_task_rules` trigger), so editing requests in the browser can't get around it.
  Schedules and notes can only be written on Max (RLS).

**Demo mode** (switch plans without paying, for presentations), in the SQL editor:

```sql
update public.app_settings set value = 'on'  where key = 'demo_mode';  -- on (default)
update public.app_settings set value = 'off' where key = 'demo_mode';  -- off for real use
```

In the app, turn on **Me > Demo mode**; the Plans buttons then switch plans instantly (`MockCheckout`).

## 4. Environment variables

| Variable | Where | Public? |
|---|---|---|
| `SUPABASE_URL` | `.env` / Vercel | yes |
| `SUPABASE_ANON_KEY` | `.env` / Vercel | yes (RLS protects data) |
| `STRIPE_PAYMENT_LINK_PRO` | `.env` / Vercel | yes (a payment page URL) |
| `STRIPE_PAYMENT_LINK_MAX` | `.env` / Vercel | yes |
| `STRIPE_SECRET_KEY` | Supabase secrets only | **no** |
| `STRIPE_WEBHOOK_SECRET` | Supabase secrets only | **no** |
| `STRIPE_PRO_LINK_ID`, `STRIPE_MAX_LINK_ID` | Supabase secrets only | ids (`plink_...`) |

The four public values are written into `dist/config.js` at build time (Maven resource filtering).
The service role key and Stripe secret key are never read by the frontend build.

## 5. Run locally

```bash
./mvnw package
npx serve dist -l 5173        # or any static server, e.g. python -m http.server -d dist 5173
```

Open http://localhost:5173. Rebuild with `./mvnw package -DskipTests` after changes.
Service workers work on `localhost`; for phones on your Wi-Fi you need HTTPS (deploy to Vercel).

## 6. Deploy to Vercel

1. Push the project to GitHub and **Import** it in Vercel (or run `vercel` in this folder).
2. Framework preset: **Other**. `vercel.json` already sets:
   - **Build command:** `bash scripts/vercel-build.sh` (downloads JDK 21, runs `./mvnw clean package`)
   - **Output directory:** `dist`
   - **Install command:** none needed (no npm packages)
3. **Settings > Environment Variables**: add the four public variables from the table.
4. Deploy. Then put the Vercel URL in Supabase (Auth > URL Configuration) and in the Stripe
   Payment Links' "after payment" redirect (see below).

Alternative: build locally and upload only the static files with `vercel deploy dist`
(env vars then come from your local `.env`).

## 7. Stripe in test mode

1. In the Stripe dashboard switch on **Test mode**.
2. Create two products with monthly prices: **Pro ₱49** and **Max ₱99** (currency PHP).
3. Create a **Payment Link** for each. Under *After payment*, choose "Don't show confirmation page",
   redirect to `https://YOUR-APP.vercel.app/?checkout=success`.
   Copy each link URL into `STRIPE_PAYMENT_LINK_PRO` / `_MAX`, and each link id (`plink_...`)
   into the function secrets.
4. Deploy the webhook function (needs the [Supabase CLI](https://supabase.com/docs/guides/cli)):
   ```bash
   supabase login
   supabase link --project-ref YOUR_REF
   cp supabase/functions/.env.example supabase/functions/.env   # fill in
   supabase secrets set --env-file supabase/functions/.env
   supabase functions deploy stripe-webhook --no-verify-jwt
   ```
5. In Stripe: **Developers > Webhooks > Add endpoint**:
   `https://YOUR_REF.supabase.co/functions/v1/stripe-webhook`, events
   `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`.
   Copy its signing secret into `STRIPE_WEBHOOK_SECRET` and run `supabase secrets set` again.
6. Test: turn **Demo mode off** in the app, open Plans, tap *Upgrade to Pro*, pay with card
   `4242 4242 4242 4242`, any future expiry, any CVC. You return to the app; the plan updates
   within a few seconds. Check the webhook's logs in Supabase > Edge Functions if it doesn't.

## 8. Install on a phone

- **Android (Chrome):** open the Vercel URL, go to **Me**, tap **Add to home screen**
  (or Chrome menu > *Install app*).
- **iPhone (Safari):** open the URL in Safari, tap **Share**, then **Add to Home Screen**.
  The Me screen shows these steps on iPhone because iOS has no install button for web apps.

After installing, the app opens full-screen and works offline: it shows the saved tasks, and
changes made offline are queued and synced when the phone is back online.

Reminders (Pro) use browser notifications through the service worker. They fire while the app
is open or in a background tab (24 h and 1 h before a deadline). Reminders with the app fully
closed would need a push server, which is outside this project. On iPhone, notifications only
work for the installed app (iOS 16.4+).

## 9. Testing

```bash
./mvnw test
```

JUnit tests (`src/test/java`) cover `TaskService` (add, due date required, complete/undo,
grouping, sorting, Free limit), `SubscriptionService` + `MockCheckout`, `BloomMeterService`,
`AuthService` validation and error messages, `ReminderService`, the plan rules of every
`Subscription` subclass, and the date math in `LocalDay`.

**3-minute demo script:** sign in > tap **+** > type the assignment > tap *Tomorrow* >
*Add assignment* > it appears under *This week* > tap its circle > petals burst, the bloom meter
gains a petal, *Undo* toast > after 5 s it moves to *Done*.

## 10. Where the OOP principles are used (for the presentation)

Each principle is also marked with a comment in the code.

**Encapsulation:** data is private and changed only through methods that keep it valid.
- `Task`: private fields; `markCompleted(now)` / `markUnfinished()` keep `completed` and
  `completedAt` consistent.
- `TaskService`: the task list is private; screens get copies and can only change tasks through
  `addTask`, `complete`, `undoComplete`, `delete`.
- `Json` wraps raw JavaScript objects behind typed getters/setters.

**Inheritance:** subclasses reuse and extend a parent.
- `FreePlan`, `ProPlan` extend `Subscription`; `MaxPlan` extends `ProPlan` ("everything in Pro, plus").
- `Screen` extends `Component`; `LoginScreen`, `TaskListScreen`, `AddTaskScreen`, `PricingScreen`,
  `SettingsScreen`, `ScheduleScreen` extend `Screen`. `TaskCard`, `BloomMeter`, `PlanCard`, `Toast`,
  `BottomSheet` extend `Component`.
- `SupabaseTaskRepository` (and the schedule/notes versions) extend `SupabaseRestRepository<T>`,
  which holds all the HTTP code; `LocalTaskRepository` extends `LocalListRepository<T>`.

**Polymorphism:** one call, different behavior depending on the object.
- `subscription.canUseReminders()`, `getMaxActiveTasks()`, `getFeatures()`: each plan answers
  differently; `PlanCard` and `Feature.isAvailableOn()` work with any of them.
- `CheckoutGateway.checkout()`: `StripeCheckout` redirects to Stripe, `MockCheckout` upgrades
  instantly. `AppServices.getCheckout()` picks one based on Demo mode.
- `TaskRepository`: `TaskService` works the same with `SupabaseTaskRepository` (wrapped by
  `OfflineTaskRepository`), `LocalTaskRepository`, or the in-memory fake in tests.
- `App.show(Screen)` renders any screen subclass.

**Abstraction:** interfaces and abstract classes hide the details.
- `TaskRepository`, `CrudRepository<T>`, `SubscriptionRepository`, `AuthGateway`,
  `CheckoutGateway`, `Notifier`, `AppClock`, `KeyValueStore` describe *what*, not *how*.
- `Subscription` and `Screen` are abstract: you can't make "a plan" or "a screen" without saying which.
- `Js` is the one small interop layer; everything above it is ordinary Java.
