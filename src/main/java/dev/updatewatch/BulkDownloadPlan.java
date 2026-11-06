package dev.updatewatch;

import java.util.*;
import java.util.function.Predicate;

/** Select only confirmed, direct-download updates from the last successful check. */
final class BulkDownloadPlan {
    record Plan(int confirmedUpdates, List<UpdateCheckService.Result> ready, int manual, int checksumBlocked, int alreadyRunning) {}

    static Plan plan(Collection<UpdateCheckService.Result> results,
                     Predicate<UpdateCheckService.Result> checksumRequired,
                     Set<String> running) {
        int confirmed = 0, manual = 0, blocked = 0, active = 0;
        List<UpdateCheckService.Result> ready = new ArrayList<>();
        for (var result : results) {
            if (!ReportVisibility.confirmedUpdate(result)) continue;
            confirmed++;
            var release = result.release();
            if (release == null || release.download() == null) {
                manual++;
            } else if (checksumRequired.test(result) && !release.hasChecksum()) {
                blocked++;
            } else if (running.contains(result.source().name().toLowerCase(Locale.ROOT))) {
                active++;
            } else {
                ready.add(result);
            }
        }
        return new Plan(confirmed, List.copyOf(ready), manual, blocked, active);
    }

    private BulkDownloadPlan() {}
}
