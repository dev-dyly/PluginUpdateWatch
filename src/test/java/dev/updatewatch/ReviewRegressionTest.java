package dev.updatewatch;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import com.google.gson.JsonParser;
import static org.junit.jupiter.api.Assertions.*;

class ReviewRegressionTest {
    @Test void staleResultsAreBlockedAfterFailureReloadOrDiskEditsForBothDownloadCommands() throws Exception {
        DownloadCache.validate(false, true, null, "config", "config");
        assertThrows(Failure.Problem.class, () -> DownloadCache.validate(true, true, null, "config", "config"));
        assertThrows(Failure.Problem.class, () -> DownloadCache.validate(false, false, null, "config", "config"));
        assertThrows(Failure.Problem.class, () -> DownloadCache.validate(false, true, "failed scan/reload", "config", "config"));
        assertThrows(Failure.Problem.class, () -> DownloadCache.validate(false, true, null, "config", "edited"));
    }
    @Test void bulkSummaryAccountsForEveryOutcomeAndRejectsDoubleCounting() {
        var progress = new BulkProgress(5);
        assertFalse(progress.completed(null));
        assertFalse(progress.completed(new Failure(Failure.Kind.ALREADY_INSTALLED, "", 0)));
        assertFalse(progress.completed(new Failure(Failure.Kind.ALREADY_DOWNLOADED, "", 0)));
        assertFalse(progress.completed(new Failure(Failure.Kind.NETWORK, "", 0)));
        assertTrue(progress.completed(new Failure(Failure.Kind.CANCELLED, "", 0)));
        assertTrue(progress.summary().contains("5/5 processed, 1 saved, 2 already saved/installed/not newer, 1 failed, 1 cancellation(s) requested"));
        assertThrows(IllegalStateException.class, () -> progress.completed(null));
    }
    @Test void localConsoleUsesOneLoggerRouteAndRconOrPlayersReceiveOneReplyAndOneAudit() {
        var logger = Logger.getAnonymousLogger(); logger.setUseParentHandlers(false);
        var records = new ArrayList<LogRecord>(); var sends = new AtomicInteger();
        logger.addHandler(new Handler() {
            public void publish(LogRecord record) { records.add(record); }
            public void flush() {} public void close() {}
        });
        MessageLog.deliver(logger, true, true, sends::incrementAndGet, "[WARNING] missing checksum");
        assertEquals(0, sends.get()); assertEquals(1, records.size()); assertEquals(Level.WARNING, records.getFirst().getLevel());
        MessageLog.deliver(logger, false, true, sends::incrementAndGet, "[ERROR] download failed");
        assertEquals(1, sends.get()); assertEquals(2, records.size()); assertEquals(Level.SEVERE, records.getLast().getLevel());
        MessageLog.deliver(logger, false, false, sends::incrementAndGet, "report reply");
        assertEquals(2, sends.get()); assertEquals(2, records.size());
    }
    @Test void scheduledReportsOnlyRepeatChangedIssuesAndAnnounceRecoveryAndRecurrence() {
        var reports = new ScanMessages();
        var first = Map.of("Source A", "[WARNING] source missing", "Plugin B", "[ERROR] network failed");
        assertEquals(2, reports.changed(first).size()); assertTrue(reports.changed(first).isEmpty());
        var changed = reports.changed(Map.of("Source A", "[WARNING] source missing", "Plugin B", "[ERROR] invalid response"));
        assertEquals(List.of("[ERROR] invalid response"), changed);
        assertEquals(2, reports.changed(Map.of()).size()); assertEquals(2, reports.changed(first).size());
    }
    @Test void wrappedCancellationRemainsCancellationButTimeoutRemainsOperationalFailure() {
        assertEquals(Failure.Kind.CANCELLED, Failure.classify(Failure.problem(Failure.Kind.NETWORK, "read failed", new InterruptedIOException())).kind());
        assertEquals(Failure.Kind.NETWORK, Failure.classify(Failure.problem(Failure.Kind.NETWORK, "timed out", new SocketTimeoutException())).kind());
        assertEquals(Level.INFO, MessageLog.level(new Failure(Failure.Kind.ALREADY_DOWNLOADED, "saved", 0).describe(null)));
        assertEquals(Level.WARNING, MessageLog.level(new Failure(Failure.Kind.NO_COMPATIBLE_RELEASE, "none", 0).describe(null)));
        assertEquals(Level.SEVERE, MessageLog.level(new Failure(Failure.Kind.INVALID_ARTIFACT, "bad hash", 0).describe(null)));
    }
    @Test void metadataIsReusedOnlyWithinTheSameScanIncludingUnavailableProviders() throws Exception {
        var calls = new AtomicInteger();
        Providers.JsonFetch remote = url -> { calls.incrementAndGet(); if (url.equals("down")) throw new Remote.HttpError(503); return JsonParser.parseString("{\"version\":2}"); };
        var scan = Providers.memoized(remote);
        var first = scan.get("up"); first.getAsJsonObject().addProperty("version", 0);
        assertEquals(2, scan.get("up").getAsJsonObject().get("version").getAsInt());
        assertThrows(Remote.HttpError.class, () -> scan.get("down")); assertThrows(Remote.HttpError.class, () -> scan.get("down"));
        assertEquals(2, calls.get()); Providers.memoized(remote).get("up"); assertEquals(3, calls.get());
    }
    @Test void hardcodedSnapshotUserAgentIsReplacedByFilteredBuildVersion() throws Exception {
        var build = new Properties();
        try (var in = getClass().getResourceAsStream("/build.properties")) { build.load(in); }
        assertEquals("PluginUpdateWatch/" + build.getProperty("version") + " (+https://github.com/dev-dyly/PluginUpdateWatch)", HttpTransport.USER_AGENT);
    }
    @Test void unrelatedProviderFailuresDoNotRenotifyAdministratorsAboutExistingUpdates() {
        var source = new Remote.Source("Example", "1", "github", "o/r", ".*", "26.3");
        var update = new UpdateCheckService.Result(source, new Remote.Release("2", "url", "page"), Versions.Status.UPDATE, null);
        var failed = new UpdateCheckService.Result(source, null, null, "[ERROR] network failed");
        assertTrue(ReportVisibility.newUpdates(Map.of(), Map.of("example", update)));
        assertFalse(ReportVisibility.newUpdates(Map.of("example", update), Map.of("example", update, "other", failed)));
        assertFalse(ReportVisibility.newUpdates(Map.of("example", update), Map.of()));
    }
}
