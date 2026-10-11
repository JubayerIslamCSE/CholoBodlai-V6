package com.jubayer.cholobodlai.Component.Pricing;

import com.jubayer.cholobodlai.Model.Trip;
import java.math.BigDecimal;

public interface FareCalculator {
    boolean appliesTo(Trip trip);
    BigDecimal estimate(Trip trip);
}