package dev.updatewatch;

/** Console reports contain actionable results; player reports retain the full inventory. */
final class ReportVisibility {
    static boolean newUpdates(java.util.Map<String, UpdateCheckService.Result> before,
                              java.util.Map<String, UpdateCheckService.Result> after) {
        // Provider failures or missing-source notes must not re-notify admins about an unchanged update.
        return after.entrySet().stream().anyMatch(entry -> confirmedUpdate(entry.getValue())
                && !entry.getValue().equals(before.get(entry.getKey())));
    }
    static boolean confirmedUpdate(UpdateCheckService.Result result) {
        return result.error() == null && result.status() == Versions.Status.UPDATE;
    }
    static boolean result(boolean console, UpdateCheckService.Result result) {
        return !console || result.error() != null || result.status() == Versions.Status.UPDATE;
    }
    static boolean note(boolean console, String note) { return !console || !note.startsWith("[INFO]"); }
    static UpdateCheckService.Result afterDownload(UpdateCheckService.Result result, Failure outcome) {
        return outcome != null && outcome.kind() == Failure.Kind.ALREADY_INSTALLED
                ? new UpdateCheckService.Result(result.source(), result.release(), Versions.Status.CURRENT, null) : result;
    }
}
