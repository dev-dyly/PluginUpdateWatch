package dev.updatewatch;

import java.util.logging.*;

/** One local console route, with Bukkit's SEVERE level representing ERROR. */
final class MessageLog {
    static Level level(String message) {
        return message.contains("[ERROR]") ? Level.SEVERE : message.contains("[WARNING]") ? Level.WARNING : Level.INFO;
    }
    static void deliver(Logger logger, boolean localConsole, boolean audit, Runnable send, String message) {
        if (!localConsole) send.run();
        if (localConsole || audit) logger.log(level(message), message);
    }
}
