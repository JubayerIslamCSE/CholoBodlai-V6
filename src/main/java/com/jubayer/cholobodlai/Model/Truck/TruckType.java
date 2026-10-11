package com.jubayer.cholobodlai.Model.Truck;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "truck_types")
public class TruckType {
    @Id
    private String id;
    private String name;
    private String categoryId;      // reference
    private String categoryName;    // snapshot
    private Double capacityTons;
    private boolean covered;
    private BigDecimal baseRatePerKm;
}
