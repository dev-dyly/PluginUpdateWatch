package dev.updatewatch;

import java.util.*;

/** Remember the last report by issue, rather than replaying every issue when any result changes. */
final class ScanMessages {
    private Map<String, String> previous = Map.of();
    List<String> changed(Map<String, String> current) {
        var changed = new ArrayList<String>();
        current.forEach((key, value) -> { if (!value.equals(previous.get(key))) changed.add(value); });
        previous.forEach((key, value) -> {
            if (!current.containsKey(key) && (value.contains("[ERROR]") || value.contains("[WARNING]")))
                changed.add("[INFO] Resolved: " + key + ". Use /pu list all for current details.");
        });
        previous = Map.copyOf(current);
        return List.copyOf(changed);
    }
}
