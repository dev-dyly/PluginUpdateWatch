package dev.updatewatch;

import java.util.Objects;

/** Both command admission and queued work must use a successful check of the loaded configuration. */
final class DownloadCache {
    static void validate(boolean checking, boolean checked, String failure, String loaded, String disk) throws Failure.Problem {
        if (checking || !checked || failure != null || !Objects.equals(loaded, disk))
            throw Failure.problem(Failure.Kind.INVALID_CONFIG,
                    "No fresh successful check is available for this configuration; run /pu reload if edited, then /pu check before downloading");
    }
}
