package dev.updatewatch;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class DownloadQueueTest {
    static class Worker extends AbstractExecutorService {
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        boolean stopped;
        public void execute(Runnable task) { if (stopped) throw new RejectedExecutionException(); tasks.add(task); }
        public void shutdown() { stopped = true; }
        public List<Runnable> shutdownNow() { stopped = true; return List.copyOf(tasks); }
        public boolean isShutdown() { return stopped; }
        public boolean isTerminated() { return stopped; }
        public boolean awaitTermination(long time, TimeUnit unit) { return stopped; }
        void runAll() { while (!tasks.isEmpty()) tasks.remove().run(); }
    }
    @Test void queuesAsynchronouslyRejectsConcurrentDuplicatesAndContinuesAfterFailure() {
        var worker = new Worker(); var callbacks = new ArrayDeque<Runnable>();
        var queue = new DownloadQueue<String>(worker, callbacks::add);
        var results = new ArrayList<DownloadQueue.Outcome<String>>();
        assertTrue(queue.submit("Alpha", () -> { throw new IOException(); }, results::add));
        assertFalse(queue.submit("ALPHA", () -> fail("Duplicate must never start"), results::add));
        assertTrue(queue.submit("Bravo", () -> "saved", results::add));
        assertTrue(results.isEmpty()); assertEquals(Set.of("alpha", "bravo"), queue.active());
        worker.runAll(); assertTrue(results.isEmpty()); // Only the server dispatcher can complete state.
        while (!callbacks.isEmpty()) callbacks.remove().run();
        assertEquals(2, results.size()); assertInstanceOf(IOException.class, results.get(0).error());
        assertEquals("saved", results.get(1).value()); assertTrue(queue.active().isEmpty());
    }
    @Test void cancelAccountsForQueuedAndFinishedWorkExactlyOnceEvenWithLateCallbacks() {
        var worker = new Worker(); var callbacks = new ArrayDeque<Runnable>();
        var queue = new DownloadQueue<String>(worker, callbacks::add);
        var results = new ArrayList<DownloadQueue.Outcome<String>>();
        queue.submit("Finished", () -> "saved", results::add);
        queue.submit("Queued", () -> fail("Cancelled queue entry must not run"), results::add);
        worker.tasks.remove().run(); // Finished, but its Bukkit callback has not run yet.
        queue.cancelAll(); assertEquals(2, results.size());
        assertEquals("saved", results.get(0).value());
        assertEquals(Failure.Kind.CANCELLED, Failure.classify(results.get(1).error()).kind());
        queue.submit("Finished", () -> "new operation", results::add);
        callbacks.remove().run(); assertEquals(Set.of("finished"), queue.active());
        worker.runAll(); while (!callbacks.isEmpty()) callbacks.remove().run();
        assertEquals(3, results.size()); assertTrue(queue.active().isEmpty());
    }
    @Test void executorRejectionCompletesAndReleasesAdmission() {
        var worker = new Worker(); worker.shutdown(); var results = new ArrayList<DownloadQueue.Outcome<String>>();
        var queue = new DownloadQueue<String>(worker, Runnable::run);
        assertTrue(queue.submit("Example", () -> fail(), results::add));
        assertEquals(1, results.size()); assertTrue(queue.active().isEmpty());
        assertEquals(Failure.Kind.CANCELLED, Failure.classify(results.getFirst().error()).kind());
    }
    @Test void disableInterruptsRunningWorkWithoutWaitingOnServerThread() throws Exception {
        var worker = Executors.newSingleThreadExecutor(); var started = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1); var callbacks = new ConcurrentLinkedQueue<Runnable>();
        var queue = new DownloadQueue<String>(worker, callbacks::add);
        var results = new ArrayList<DownloadQueue.Outcome<String>>();
        try {
            queue.submit("Example", () -> {
                started.countDown();
                try { new CountDownLatch(1).await(); return "unexpected"; }
                catch (InterruptedException e) { interrupted.countDown(); throw e; }
            }, results::add);
            assertTrue(started.await(5, TimeUnit.SECONDS));
            queue.cancelAll();
            assertEquals(1, results.size()); assertTrue(queue.active().isEmpty());
            assertTrue(interrupted.await(5, TimeUnit.SECONDS));
            worker.shutdown(); assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
            while (!callbacks.isEmpty()) callbacks.remove().run();
            assertEquals(1, results.size());
        } finally { worker.shutdownNow(); }
    }
    @Test void linkageFailureStillCompletesAndRemainingJobsRun() {
        var worker = new Worker(); var callbacks = new ArrayDeque<Runnable>();
        var queue = new DownloadQueue<String>(worker, callbacks::add);
        var results = new ArrayList<DownloadQueue.Outcome<String>>();
        queue.submit("Broken", () -> { throw new NoClassDefFoundError("fixture"); }, results::add);
        queue.submit("Good", () -> "saved", results::add);
        worker.runAll(); while (!callbacks.isEmpty()) callbacks.remove().run();
        assertEquals(Failure.Kind.UNEXPECTED, Failure.classify(results.getFirst().error()).kind());
        assertEquals("saved", results.getLast().value()); assertTrue(queue.active().isEmpty());
    }
}
