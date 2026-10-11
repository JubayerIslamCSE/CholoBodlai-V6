package com.jubayer.cholobodlai.Model;

import com.jubayer.cholobodlai.Model.Emums.BidStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "bids")
public class Bid {
    @Id
    private String id;
    private String tripId;                 // reference
    private String ownerId;                // reference
    private String ownerName;              // snapshot
    private String truckId;                // reference
    private String truckRegistrationNo;    // snapshot
    private BigDecimal amount;
    private BidStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
