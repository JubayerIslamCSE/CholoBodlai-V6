package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import java.util.Locale;

/** Shared bounds for untrusted search/filter query parameters. */
public final class InputPolicy {
    private InputPolicy() {}

    public static String searchTerm(String raw) {
        if (raw == null) return "";
        String value = raw.strip();
        if (value.length() > 80) {
            throw new BusinessRuleException("Search filters must contain at most 80 characters");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    /** Allow only declared trip/bid states, never arbitrary filters. */
    public static String statusTerm(String raw, java.util.Set<String> allowed) {
        if (raw == null || raw.isBlank()) return "";
        if (raw.length() > 32) throw new BusinessRuleException("Invalid status filter");
        String state = raw.strip().toUpperCase(Locale.ROOT);
        if (!state.equals("ALL") && !allowed.contains(state))
            throw new BusinessRuleException("Choose a valid status from the list");
        return state;
    }
}
