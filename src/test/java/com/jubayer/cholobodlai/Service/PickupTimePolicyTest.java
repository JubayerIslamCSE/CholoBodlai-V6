package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;

class PickupTimePolicyTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Dhaka");
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"), ZONE);
    private final PickupTimePolicy policy = new PickupTimePolicy(FIXED);
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 12, 6, 0);

    @Test void exactly48HoursAheadIsAccepted() {
        assertDoesNotThrow(() -> policy.validate(now.plusDays(2)));
    }

    @Test void pastDateIsRejected() {
        assertThrows(BusinessRuleException.class, () -> policy.validate(now.minusDays(1)));
    }

    @Test void dateOneMinuteShortOf48HoursIsRejected() {
        assertThrows(BusinessRuleException.class, () -> policy.validate(now.plusDays(2).minusMinutes(1)));
    }

    @Test void missingDateIsRejected() {
        assertThrows(BusinessRuleException.class, () -> policy.validate(null));
    }

    @Test void datePickerMinimumIsSafelyRoundedUp() {
        LocalDateTime min = policy.earliestAllowedForForm();
        assertFalse(min.isBefore(now.plusDays(2)));
        assertDoesNotThrow(() -> policy.validate(min));
    }
}
