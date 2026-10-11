package com.jubayer.cholobodlai.Model.Truck;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "truck_categories")
public class TruckCategory {
    @Id
    private String id;
    @Indexed(unique = true)
    private String name;
    private String description;
}
