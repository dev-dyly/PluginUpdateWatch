package dev.updatewatch;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BulkDownloadPlanTest {
    private UpdateCheckService.Result result(String name, Versions.Status status, String download,
                                             boolean checksum, String error) {
        var source = new Remote.Source(name, "1.0", "modrinth", "project", null, "26.3");
        var release = new Remote.Release("2.0", download, "https://modrinth.com/plugin/example",
                checksum ? "a".repeat(128) : null);
        return new UpdateCheckService.Result(source, release, status, error);
    }

    @Test void queuesOnlyConfirmedUpdatesNotUnconfirmedOrFailedResults() {
        var update = result("Good", Versions.Status.UPDATE, "https://example.org/good.jar", true, null);
        var current = result("Current", Versions.Status.CURRENT, "https://example.org/cur.jar", true, null);
        var unknown = result("Unknown", Versions.Status.UNKNOWN, "https://example.org/un.jar", true, null);
        var different = result("Different", Versions.Status.DIFFERENT, "https://example.org/diff.jar", true, null);
        var failed = result("Failed", Versions.Status.UPDATE, "https://example.org/fail.jar", true, "Provider failed");
        var plan = BulkDownloadPlan.plan(List.of(update, current, unknown, different, failed), r -> true, Set.of());
        assertEquals(1, plan.confirmedUpdates());
        assertEquals(List.of(update), plan.ready());
        assertEquals(0, plan.manual());
        assertEquals(0, plan.checksumBlocked());
        assertEquals(0, plan.alreadyRunning());
    }

    @Test void skipsMissingUrlsChecksumsAndAlreadyRunningDownloads() {
        var first = result("Alpha", Versions.Status.UPDATE, "https://example.org/a.jar", true, null);
        var second = result("Bravo", Versions.Status.UPDATE, "https://example.org/b.jar", false, null);
        var manual = result("Manual", Versions.Status.UPDATE, null, true, null);
        var running = result("AlreadyQueued", Versions.Status.UPDATE, "https://example.org/d.jar", true, null);
        var last = result("Charlie", Versions.Status.UPDATE, "https://example.org/e.jar", true, null);
        var plan = BulkDownloadPlan.plan(List.of(first, second, manual, running, last), r -> true, Set.of("alreadyqueued"));
        assertEquals(5, plan.confirmedUpdates());
        assertEquals(List.of(first, last), plan.ready());
        assertEquals(1, plan.manual());
        assertEquals(1, plan.checksumBlocked());
        assertEquals(1, plan.alreadyRunning());
    }

    @Test void honoursExplicitPolicyAllowingMissingChecksums() {
        var unverified = result("Web", Versions.Status.UPDATE, "https://example.org/web.jar", false, null);
        var plan = BulkDownloadPlan.plan(List.of(unverified), r -> false, Set.of());
        assertEquals(List.of(unverified), plan.ready());
        assertEquals(0, plan.checksumBlocked());
    }

    @Test void doesNotQueueNullReleaseEvenIfStatusClaimsUpdate() {
        var invalid = new UpdateCheckService.Result(
                new Remote.Source("Absent", "1.0", "github", "owner/repo", null, "26.3"),
                null, Versions.Status.UPDATE, null);
        var plan = BulkDownloadPlan.plan(List.of(invalid), r -> true, Set.of());
        assertEquals(1, plan.confirmedUpdates());
        assertEquals(1, plan.manual());
        assertTrue(plan.ready().isEmpty());
    }
}
