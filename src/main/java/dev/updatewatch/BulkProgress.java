package dev.updatewatch;

/** Server-thread completion accounting, including queued operations cancelled before they start. */
final class BulkProgress {
    private final int total;
    private int completed, saved, unchanged, failed, cancelled;
    BulkProgress(int total) { this.total = total; }
    boolean completed(Failure failure) {
        if (completed >= total) throw new IllegalStateException("Bulk completion counted twice");
        completed++;
        if (failure == null) saved++;
        else if (failure.noUpdate()) unchanged++;
        else if (failure.kind() == Failure.Kind.CANCELLED) cancelled++;
        else failed++;
        return completed == total;
    }
    String summary() {
        return "[INFO] downloadall complete: " + completed + "/" + total + " processed, " + saved + " saved, " + unchanged
                + " already saved/installed/not newer, " + failed + " failed, " + cancelled
                + " cancellation(s) requested. Review plugins/PluginUpdateWatch/downloads/ before installing.";
    }
}
