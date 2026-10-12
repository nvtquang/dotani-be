package com.dotani.audit.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the "details" of an audit log: only fields whose value changed, as
 * {"field": {"before": ..., "after": ...}}. Values are stored as strings so dates and enums read the
 * same everywhere. Callers pass business fields only, never contact or bank data.
 */
public final class AuditDiff {

    private AuditDiff() {
    }

    /** Snapshot from alternating name/value pairs: snapshot("title", t, "status", s). Null values are kept. */
    public static Map<String, Object> snapshot(Object... namesAndValues) {
        if (namesAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("snapshot expects name/value pairs");
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            Object value = namesAndValues[i + 1];
            snapshot.put((String) namesAndValues[i], value == null ? null : String.valueOf(value));
        }
        return snapshot;
    }

    public static Map<String, Object> changes(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        java.util.Set<String> fields = new java.util.LinkedHashSet<>(before.keySet());
        fields.addAll(after.keySet());
        for (String field : fields) {
            Object oldValue = before.get(field);
            Object newValue = after.get(field);
            if (!Objects.equals(oldValue, newValue)) {
                Map<String, Object> change = new LinkedHashMap<>();
                change.put("before", oldValue);
                change.put("after", newValue);
                changes.put(field, change);
            }
        }
        return changes;
    }

    /** Details for a created resource: every field goes from null to its value. */
    public static Map<String, Object> created(Map<String, Object> after) {
        return changes(Map.of(), after);
    }

    /** Details for a deleted resource: every field goes from its value to null. */
    public static Map<String, Object> deleted(Map<String, Object> before) {
        return changes(before, Map.of());
    }
}
