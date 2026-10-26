package app.studybloom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.studybloom.fakes.FakeSubscriptionRepository;
import app.studybloom.model.Feature;
import app.studybloom.model.MaxPlan;
import app.studybloom.model.PlanType;
import app.studybloom.model.ProPlan;
import app.studybloom.model.Student;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionServiceTest {
    private FakeSubscriptionRepository repository;
    private SubscriptionService service;
    private final Student bea = new Student("bea", "bea.santos@school.edu.ph");

    @BeforeEach
    void setUp() {
        repository = new FakeSubscriptionRepository();
        service = new SubscriptionService(repository);
    }

    @Test
    void startsOnFree() {
        assertEquals(PlanType.FREE, service.getCurrent().getPlanType());
        assertEquals("You're using 12 of 15 free tasks.", service.usageText(12));
        assertEquals(PlanType.PRO, service.recommendedUpgrade());
        assertFalse(service.isAvailable(Feature.REMINDERS));
    }

    @Test
    void loadUsesTheStoredPlan() {
        repository.setPlan(PlanType.MAX);
        service.load("bea", () -> { }, e -> { });
        assertInstanceOf(MaxPlan.class, service.getCurrent());
        assertNull(service.recommendedUpgrade());
        assertTrue(service.isAvailable(Feature.NOTES));
    }

    @Test
    void mockCheckoutUpgradesInstantlyAndNotifiesListeners() {
        AtomicInteger changes = new AtomicInteger();
        service.addChangeListener(changes::incrementAndGet);
        CheckoutGateway mock = new MockCheckout(repository, service);
        AtomicReference<String> error = new AtomicReference<>();

        service.upgrade(bea, PlanType.PRO, mock, () -> { }, error::set);

        assertNull(error.get());
        assertInstanceOf(ProPlan.class, service.getCurrent());
        assertTrue(service.getCurrent().isDemo());
        assertEquals(PlanType.PRO, repository.getPlan());
        assertEquals(1, changes.get());
        assertTrue(mock.isInstant());
    }

    @Test
    void mockCheckoutReportsWhenServerDemoIsOff() {
        repository.setDemoAllowed(false);
        AtomicReference<String> error = new AtomicReference<>();
        service.upgrade(bea, PlanType.MAX, new MockCheckout(repository, service), () -> { }, error::set);
        assertTrue(error.get().contains("Demo mode"));
        assertEquals(PlanType.FREE, service.getCurrent().getPlanType());
    }

    @Test
    void upgradingToTheCurrentPlanIsRefused() {
        AtomicReference<String> error = new AtomicReference<>();
        service.upgrade(bea, PlanType.FREE, new MockCheckout(repository, service), () -> { }, error::set);
        assertEquals("You're already on Free.", error.get());
    }

    @Test
    void allPlansAreListedCheapestFirst() {
        assertEquals(3, service.allPlans().size());
        assertEquals(PlanType.FREE, service.allPlans().get(0).getPlanType());
        assertEquals(PlanType.MAX, service.allPlans().get(2).getPlanType());
    }
}
