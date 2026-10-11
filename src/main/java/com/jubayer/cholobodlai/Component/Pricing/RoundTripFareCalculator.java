package com.jubayer.cholobodlai.Component.Pricing;

import com.jubayer.cholobodlai.Model.Trip;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class RoundTripFareCalculator implements FareCalculator {
    private static final BigDecimal RETURN_DISCOUNT = BigDecimal.valueOf(0.90);
    @Override public boolean appliesTo(Trip trip) { return trip.isRoundTrip(); }
    @Override public BigDecimal estimate(Trip trip) {
        BigDecimal oneWay = trip.getBaseRatePerKm().multiply(BigDecimal.valueOf(trip.getDistanceKm()));
        return oneWay.add(oneWay.multiply(RETURN_DISCOUNT)).setScale(2, RoundingMode.HALF_UP);
    }
}