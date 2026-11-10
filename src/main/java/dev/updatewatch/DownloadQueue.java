package dev.updatewatch;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Admission and completion are server-thread owned; only the callable runs on the I/O worker. */
final class DownloadQueue<T> {
    record Outcome<T>(T value, Throwable error) {}
    private final ExecutorService worker;
    private final Consumer<Runnable> dispatch;
    private final Map<String, Job> jobs = new LinkedHashMap<>();
    private final class Job {
        FutureTask<Void> future;
        volatile Outcome<T> outcome;
        Consumer<Outcome<T>> complete;
    }

    DownloadQueue(ExecutorService worker, Consumer<Runnable> dispatch) {
        this.worker = worker;
        this.dispatch = dispatch;
    }
    Set<String> active() { return Set.copyOf(jobs.keySet()); }

    boolean submit(String name, Callable<T> work, Consumer<Outcome<T>> complete) {
        String key = name.toLowerCase(Locale.ROOT);
        if (jobs.containsKey(key)) return false;
        var job = new Job();
        job.complete = complete;
        job.future = new FutureTask<>(() -> {
            try { job.outcome = new Outcome<>(work.call(), null); }
            catch (Exception | LinkageError e) { job.outcome = new Outcome<>(null, e); }
            dispatch.accept(() -> finish(key, job, job.outcome));
            return null;
        });
        jobs.put(key, job);
        try { worker.execute(job.future); }
        catch (RejectedExecutionException e) { finish(key, job, new Outcome<>(null, e)); }
        return true;
    }

    private void finish(String key, Job job, Outcome<T> outcome) {
        if (!jobs.remove(key, job)) return; // Cancellation or a newer operation already owns this key.
        job.complete.accept(outcome);
    }

    void cancelAll() {
        for (var entry : new ArrayList<>(jobs.entrySet())) {
            var job = entry.getValue();
            job.future.cancel(true);
            var outcome = job.outcome;
            finish(entry.getKey(), job, outcome != null ? outcome : new Outcome<>(null,
                    Failure.problem(Failure.Kind.CANCELLED,
                            "Download cancellation requested by reload/disable; review the downloads folder before retrying")));
        }
    }
}
