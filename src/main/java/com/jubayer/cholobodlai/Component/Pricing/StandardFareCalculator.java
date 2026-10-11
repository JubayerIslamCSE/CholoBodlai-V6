package com.jubayer.cholobodlai.Component.Pricing;

import com.jubayer.cholobodlai.Model.Trip;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class StandardFareCalculator implements FareCalculator {
    @Override public boolean appliesTo(Trip trip) { return !trip.isRoundTrip(); }
    @Override public BigDecimal estimate(Trip trip) {
        return trip.getBaseRatePerKm()
                .multiply(BigDecimal.valueOf(trip.getDistanceKm()))
                .setScale(2, RoundingMode.HALF_UP);
    }
}