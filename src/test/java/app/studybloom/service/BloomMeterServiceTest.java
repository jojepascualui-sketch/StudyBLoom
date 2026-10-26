package app.studybloom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.studybloom.fakes.FixedClock;
import app.studybloom.model.Task;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BloomMeterServiceTest {
    private final FixedClock clock = new FixedClock();
    private final BloomMeterService bloom = new BloomMeterService(clock);
    private int ids;

    private Task task(long due, boolean completed, long completedAt) {
        Task task = new Task("t" + ids++, "bea", "Task", due);
        if (completed) {
            task.markCompleted(completedAt);
        }
        return task;
    }

    @Test
    void onePetalPerTwentyPercent() {
        assertEquals(0, BloomMeterService.petalsFor(0, 5));
        assertEquals(1, BloomMeterService.petalsFor(1, 5));
        assertEquals(3, BloomMeterService.petalsFor(3, 5));
        assertEquals(5, BloomMeterService.petalsFor(5, 5));
        assertEquals(2, BloomMeterService.petalsFor(1, 2));
        assertEquals(1, BloomMeterService.petalsFor(1, 4));
        assertEquals(0, BloomMeterService.petalsFor(0, 0));
    }

    @Test
    void threeOfFiveLikeTheMockup() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tasks.add(task(clock.at(1, 9, 0), true, clock.nowMillis()));
        }
        tasks.add(task(clock.at(0, 23, 59), false, 0));
        tasks.add(task(clock.at(3, 23, 59), false, 0));

        BloomProgress progress = bloom.compute(tasks);
        assertEquals("3 of 5 done this week", progress.getHeadline());
        assertEquals("Two more and your flower is in full bloom.", progress.getMessage());
        assertEquals(3, progress.getFilledPetals());
        assertEquals("3/5", progress.getCenterLabel());
    }

    @Test
    void fourOfFiveSaysOneMorePetal() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(task(clock.at(1, 9, 0), true, clock.nowMillis()));
        }
        tasks.add(task(clock.at(3, 23, 59), false, 0));
        BloomProgress progress = bloom.compute(tasks);
        assertEquals(4, progress.getFilledPetals());
        assertEquals("One more petal to go.", progress.getMessage());
    }

    @Test
    void ignoresLastWeeksCompletionsAndNextWeeksTasks() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task(clock.at(-5, 9, 0), true, clock.at(-3, 9, 0)));   // done last Friday
        tasks.add(task(clock.at(9, 9, 0), false, 0));                    // due next week
        tasks.add(task(clock.at(-2, 9, 0), false, 0));                   // overdue counts
        BloomProgress progress = bloom.compute(tasks);
        assertEquals(0, progress.getDone());
        assertEquals(1, progress.getTotal());
        assertEquals(0, progress.getFilledPetals());
    }

    @Test
    void allDoneIsFullBloom() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task(clock.at(1, 9, 0), true, clock.nowMillis()));
        BloomProgress progress = bloom.compute(tasks);
        assertTrue(progress.isFullBloom());
    }

    @Test
    void emptyWeek() {
        BloomProgress progress = bloom.compute(new ArrayList<>());
        assertEquals(0, progress.getFilledPetals());
        assertEquals("Nothing due this week", progress.getHeadline());
    }
}
