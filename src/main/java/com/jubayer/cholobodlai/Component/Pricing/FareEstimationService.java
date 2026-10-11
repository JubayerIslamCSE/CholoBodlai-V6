package com.jubayer.cholobodlai.Component.Pricing;

import com.jubayer.cholobodlai.Model.Trip;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FareEstimationService {

    private final List<FareCalculator> calculators;

    public FareEstimationService(List<FareCalculator> calculators) {
        this.calculators = calculators;
    }

    public BigDecimal estimate(Trip trip) {
        return calculators.stream()
                .filter(c -> c.appliesTo(trip))
                .findFirst()
                .map(c -> c.estimate(trip))
                .orElse(BigDecimal.ZERO);
    }
}