package com.jubayer.cholobodlai.Model.Truck;

import com.jubayer.cholobodlai.Model.Emums.TruckStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "trucks")
public class Truck {
    @Id
    private String id;
    @Indexed(unique = true)
    private String registrationNo;
    private String ownerId;         // reference
    private String ownerName;       // snapshot
    private String truckTypeId;     // reference
    private String truckTypeName;   // snapshot
    private String categoryName;    // snapshot
    private TruckStatus status;
    private boolean verified;
    private String documentPath;
    private String documentName;
    private String photoPath;

}
