package app.studybloom;

import app.studybloom.repo.IdGenerator;
import app.studybloom.repo.KeyValueStore;
import app.studybloom.repo.NoteRepository;
import app.studybloom.repo.ScheduleRepository;
import app.studybloom.repo.SubscriptionRepository;
import app.studybloom.repo.TaskRepository;
import app.studybloom.service.AuthGateway;
import app.studybloom.service.AuthService;
import app.studybloom.service.BloomMeterService;
import app.studybloom.service.CheckoutGateway;
import app.studybloom.service.MockCheckout;
import app.studybloom.service.Preferences;
import app.studybloom.service.ReminderService;
import app.studybloom.service.SubscriptionService;
import app.studybloom.service.TaskService;
import app.studybloom.time.AppClock;
import app.studybloom.web.AppConfig;
import app.studybloom.web.BrowserClock;
import app.studybloom.web.BrowserNotifier;
import app.studybloom.web.HttpClient;
import app.studybloom.web.LocalAuthGateway;
import app.studybloom.web.LocalNoteRepository;
import app.studybloom.web.LocalScheduleRepository;
import app.studybloom.web.LocalStorageStore;
import app.studybloom.web.LocalSubscriptionRepository;
import app.studybloom.web.LocalTaskRepository;
import app.studybloom.web.OfflineTaskRepository;
import app.studybloom.web.StripeCheckout;
import app.studybloom.web.SupabaseAuthGateway;
import app.studybloom.web.SupabaseClient;
import app.studybloom.web.SupabaseNoteRepository;
import app.studybloom.web.SupabaseScheduleRepository;
import app.studybloom.web.SupabaseSubscriptionRepository;
import app.studybloom.web.SupabaseTaskRepository;
import app.studybloom.web.UuidGenerator;

/**
 * Builds every service once and hands them out.
 *
 * POLYMORPHISM in one place: with a Supabase config it plugs in the Supabase
 * repositories; without one it plugs in the Local* ones. Nothing else in the
 * app changes, because everything else depends on the interfaces.
 */
public class AppServices {
    private final AppConfig config;
    private final KeyValueStore store;
    private final AppClock clock;
    private final IdGenerator ids;
    private final Preferences preferences;
    private final SupabaseClient supabase;
    private final OfflineTaskRepository offlineTasks;
    private final AuthService auth;
    private final SubscriptionService subscriptions;
    private final TaskService tasks;
    private final BloomMeterService bloom;
    private final ReminderService reminders;
    private final ScheduleRepository schedules;
    private final NoteRepository notes;
    private final CheckoutGateway mockCheckout;
    private final CheckoutGateway stripeCheckout;

    public AppServices(AppConfig config) {
        this.config = config;
        this.store = new LocalStorageStore();
        this.clock = new BrowserClock();
        this.ids = new UuidGenerator();
        this.preferences = new Preferences(store);

        TaskRepository taskRepository;
        SubscriptionRepository subscriptionRepository;
        AuthGateway authGateway;
        if (config.isSupabaseConfigured()) {
            supabase = new SupabaseClient(config.getSupabaseUrl(), config.getSupabaseAnonKey(), new HttpClient(), store);
            offlineTasks = new OfflineTaskRepository(new SupabaseTaskRepository(supabase), store);
            taskRepository = offlineTasks;
            subscriptionRepository = new SupabaseSubscriptionRepository(supabase, store);
            authGateway = new SupabaseAuthGateway(supabase);
            schedules = new SupabaseScheduleRepository(supabase);
            notes = new SupabaseNoteRepository(supabase);
        } else {
            supabase = null;
            offlineTasks = null;
            taskRepository = new LocalTaskRepository(store);
            subscriptionRepository = new LocalSubscriptionRepository(store);
            authGateway = new LocalAuthGateway(store);
            schedules = new LocalScheduleRepository(store);
            notes = new LocalNoteRepository(store);
        }

        this.auth = new AuthService(authGateway);
        this.subscriptions = new SubscriptionService(subscriptionRepository);
        this.tasks = new TaskService(taskRepository, subscriptions, clock, ids);
        this.bloom = new BloomMeterService(clock);
        this.reminders = new ReminderService(clock, store, new BrowserNotifier(), subscriptions, preferences);
        this.mockCheckout = new MockCheckout(subscriptionRepository, subscriptions);
        this.stripeCheckout = new StripeCheckout(config);
    }

    public boolean isLocalMode() {
        return supabase == null;
    }

    /** Demo mode decides which checkout is used. POLYMORPHISM: both are CheckoutGateways. */
    public CheckoutGateway getCheckout() {
        return preferences.isDemoMode() || isLocalMode() ? mockCheckout : stripeCheckout;
    }

    public boolean isStripeConfigured() {
        return !config.getProPaymentLink().isEmpty() && !config.getMaxPaymentLink().isEmpty();
    }

    public AppConfig getConfig() {
        return config;
    }

    public KeyValueStore getStore() {
        return store;
    }

    public AppClock getClock() {
        return clock;
    }

    public IdGenerator getIds() {
        return ids;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public SupabaseClient getSupabase() {
        return supabase;
    }

    /** Null in local mode. */
    public OfflineTaskRepository getOfflineTasks() {
        return offlineTasks;
    }

    public AuthService getAuth() {
        return auth;
    }

    public SubscriptionService getSubscriptions() {
        return subscriptions;
    }

    public TaskService getTasks() {
        return tasks;
    }

    public BloomMeterService getBloom() {
        return bloom;
    }

    public ReminderService getReminders() {
        return reminders;
    }

    public ScheduleRepository getSchedules() {
        return schedules;
    }

    public NoteRepository getNotes() {
        return notes;
    }
}
