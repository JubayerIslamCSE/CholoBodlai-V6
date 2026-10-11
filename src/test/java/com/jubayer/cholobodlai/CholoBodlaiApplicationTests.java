package com.jubayer.cholobodlai;

import com.jubayer.cholobodlai.Component.Pricing.RoundTripFareCalculator;
import com.jubayer.cholobodlai.Component.Pricing.StandardFareCalculator;
import com.jubayer.cholobodlai.Model.Trip;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/** Fast, database-free tests. Live Atlas workflow tests are in the README checklist. */
class CholoBodlaiApplicationTests {
    private Trip example(boolean roundTrip) {
        return Trip.builder().baseRatePerKm(new BigDecimal("70"))
                .distanceKm(25.0).roundTrip(roundTrip).build();
    }
    @Test void oneWayFareUsesRateTimesDistance() {
        assertEquals(new BigDecimal("1750.00"), new StandardFareCalculator().estimate(example(false)));
    }
    @Test void roundTripFareIncludesDiscountedReturn() {
        assertEquals(new BigDecimal("3325.00"), new RoundTripFareCalculator().estimate(example(true)));
    }
    @Test void calculatorSelection() {
        assertTrue(new StandardFareCalculator().appliesTo(example(false)));
        assertFalse(new StandardFareCalculator().appliesTo(example(true)));
        assertTrue(new RoundTripFareCalculator().appliesTo(example(true)));
    }
}
