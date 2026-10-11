
package com.jubayer.cholobodlai.JustToTest;

import com.jubayer.cholobodlai.Model.Truck.*;
import com.jubayer.cholobodlai.Repository.Truck.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(0)
public class TruckTypeBootstrap implements CommandLineRunner {

    private final TruckCategoryRepository categories;
    private final TruckTypeRepository types;

    public TruckTypeBootstrap(
            TruckCategoryRepository categories,
            TruckTypeRepository types) {

        this.categories = categories;
        this.types = types;
    }

    @Override
    public void run(String... args) {

        // Preserve the truck types already in MongoDB.
        if (types.count() > 0) {
            return;
        }

        add("Pickup", "Small trucks",
                "7 Feet 1 Ton", 1.0, "35");

        add("Medium", "Medium goods",
                "14 Feet 3.5 Ton", 3.5, "70");

        add("Large", "Large loads",
                "16 Feet 7.5 Ton", 7.5, "110");

        add("Trailer", "Heavy loads",
                "Flat-bed Trailer", 25.0, "220");
    }

    private void add(
            String category,
            String description,
            String name,
            double tons,
            String rate) {

        TruckCategory c =
                categories.findByName(category)
                        .orElseGet(() ->
                                categories.save(
                                        TruckCategory.builder()
                                                .name(category)
                                                .description(description)
                                                .build()
                                )
                        );

        types.save(
                TruckType.builder()
                        .name(name)
                        .categoryId(c.getId())
                        .categoryName(c.getName())
                        .capacityTons(tons)
                        .baseRatePerKm(new BigDecimal(rate))
                        .build()
        );
    }
}