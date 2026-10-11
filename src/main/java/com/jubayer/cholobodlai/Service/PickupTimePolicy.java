package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Enforces 48 hours of booking notice in Bangladesh local time, even for REST API requests. */
@Component
public class PickupTimePolicy {
    private static final ZoneId BANGLADESH = ZoneId.of("Asia/Dhaka");
    private final Clock clock;

    public PickupTimePolicy() {
        this(Clock.system(BANGLADESH));
    }

    // Fixed clock for deterministic unit tests; package-private by design.
    PickupTimePolicy(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public LocalDateTime earliestAllowedForForm() {
        // HTML datetime-local has minute precision. Round UP so its min is never
        // slightly earlier than the server-side 48-hour notice requirement.
        return LocalDateTime.now(clock).plusDays(2)
                .truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
    }

    public void validate(LocalDateTime loadingTime) {
        if (loadingTime == null) {
            throw new BusinessRuleException("Select a pickup date and time at least 48 hours ahead");
        }
        if (loadingTime.isBefore(LocalDateTime.now(clock).plusDays(2))) {
            throw new BusinessRuleException(
                    "Pickup date and time must be at least 48 hours (2 days) in the future");
        }
    }
}
