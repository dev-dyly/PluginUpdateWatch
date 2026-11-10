package dev.updatewatch;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DownloadRecoveryTest {
    @TempDir Path folder;

    @Test void recoversOnlyOwnedIncompleteFilesAndRetainsSavedArtifactsAndUnrelatedFiles() throws Exception {
        Path incomplete = Files.createTempFile(folder, ".download-", ".tmp");
        Files.writeString(incomplete, "partial transfer");
        Path saved = Files.writeString(folder.resolve("Plugin-2.0.jar"), "saved artifact");
        Path other = Files.writeString(folder.resolve("notes.tmp"), "unrelated");
        Path lookalike = Files.writeString(folder.resolve(".download-not-owned.tmp"), "unrelated");
        Path directory = Files.createDirectory(folder.resolve(".download-123.tmp"));
        assertEquals(1, DownloadManager.cleanupIncomplete(folder));
        assertFalse(Files.exists(incomplete));
        assertEquals("saved artifact", Files.readString(saved));
        assertEquals("unrelated", Files.readString(other));
        assertEquals("unrelated", Files.readString(lookalike));
        assertTrue(Files.isDirectory(directory));
        assertEquals(0, DownloadManager.cleanupIncomplete(folder));
    }

    @Test void freshInstallNeedsNoDownloadsDirectoryAndRecoveryDoesNotCreateOne() throws Exception {
        Path absent = folder.resolve("downloads");
        assertEquals(0, DownloadManager.cleanupIncomplete(absent));
        assertFalse(Files.exists(absent));
    }
}
