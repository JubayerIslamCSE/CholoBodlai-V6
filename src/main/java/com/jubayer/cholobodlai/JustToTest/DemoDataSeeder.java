package com.jubayer.cholobodlai.JustToTest;

import com.jubayer.cholobodlai.Model.*;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Model.Truck.*;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Repository.Truck.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Explicit opt-in, EMPTY DATABASE only. Never clears or rewrites existing users. */
@Component
@Order(10)
public class DemoDataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final TruckRepository trucks;
    private final TruckTypeRepository types;
    private final TripRepository trips;
    private final BidRepository bids;
    private final PasswordEncoder encoder;
    @Value("${app.demo.seed:false}") private boolean enabled;

    public DemoDataSeeder(UserRepository users, TruckRepository trucks, TruckTypeRepository types,
                          TripRepository trips, BidRepository bids, PasswordEncoder encoder) {
        this.users=users; this.trucks=trucks; this.types=types; this.trips=trips;
        this.bids=bids; this.encoder=encoder;
    }
    @Override
    public void run(String... args) {
        if (!enabled || users.count() != 0) return;
        List<TruckType> catalog = types.findAll();
        if (catalog.isEmpty()) throw new IllegalStateException("Demo seeder requires truck types");
        TruckType small = catalog.get(0);
        TruckType large = catalog.get(Math.min(1, catalog.size()-1));
        TruckType medium = catalog.get(Math.min(2, catalog.size()-1));
        User shipper = person("Nusrat Rahman", "01710000001", Role.SHIPPER, true);
        User another = person("Samiul Hasan", "01710000002", Role.SHIPPER, true);
        User ownerA = person("Karim Transport", "01820000001", Role.OWNER, true);
        User ownerB = person("Farhan Logistics", "01820000002", Role.OWNER, true);
        User ownerC = person("Rafiq Movers", "01820000003", Role.OWNER, true);
        User pending = person("Alif Cargo", "01820000004", Role.OWNER, false);
        Truck truckA = truck("DHK-METRO-11-2381", ownerA, small, TruckStatus.AVAILABLE, true);
        truck("DHK-METRO-12-1192", ownerB, large, TruckStatus.AVAILABLE, true);
        Truck truckC = truck("DHK-METRO-13-7744", ownerC, medium, TruckStatus.ON_TRIP, true);
        truck("DHK-METRO-15-3349", pending, small, TruckStatus.INACTIVE, false);
        Trip open = trip(shipper, small, "Dhanmondi, Dhaka", "Narayanganj", 28, "Furniture and boxes", TripStatus.OPEN, null);
        trip(another, large, "Uttara, Dhaka", "Gazipur", 31, "Shop goods and equipment", TripStatus.OPEN, null);
        Trip accepted = trip(shipper, medium, "Mirpur, Dhaka", "Savar, Dhaka", 34,
                "Office furniture", TripStatus.ASSIGNED, truckC.getId());
        bids.save(Bid.builder().id("bid:" + accepted.getId() + ":" + truckC.getId())
                .tripId(accepted.getId()).ownerId(ownerC.getId()).ownerName(ownerC.getFullName())
                .truckId(truckC.getId()).truckRegistrationNo(truckC.getRegistrationNo())
                .amount(new BigDecimal("3100")).status(BidStatus.ACCEPTED)
                .createdAt(LocalDateTime.now()).build());
        bids.save(Bid.builder().id("bid:" + open.getId() + ":" + truckA.getId())
                .tripId(open.getId()).ownerId(ownerA.getId()).ownerName(ownerA.getFullName())
                .truckId(truckA.getId()).truckRegistrationNo(truckA.getRegistrationNo())
                .amount(new BigDecimal("1550")).status(BidStatus.PENDING)
                .createdAt(LocalDateTime.now()).build());
    }
    private User person(String name, String phone, Role role, boolean active) {
        User u = User.builder().fullName(name).phone(phone).email(phone + "@example.invalid")
                .password(encoder.encode("Demo2026!"))
                .role(role).enabled(active)
                .approvalStatus(active ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .approvedAt(active ? LocalDateTime.now() : null).build();
        return users.save(u);
    }
    private Truck truck(String reg, User owner, TruckType type, TruckStatus status, boolean verified) {
        return trucks.save(Truck.builder().id("owner-truck:" + owner.getId())
                .ownerId(owner.getId()).ownerName(owner.getFullName())
                .registrationNo(reg).truckTypeId(type.getId()).truckTypeName(type.getName())
                .categoryName(type.getCategoryName()).status(status).verified(verified).build());
    }
    private Trip trip(User shipper, TruckType type, String from, String to, double km,
                      String goods, TripStatus status, String truckId) {
        return trips.save(Trip.builder().shipperId(shipper.getId()).shipperName(shipper.getFullName())
                .requestedTypeId(type.getId()).requestedTypeName(type.getName()).baseRatePerKm(type.getBaseRatePerKm())
                .pickupLocation(from).dropoffLocation(to).distanceKm(km).productDetails(goods)
                .estimatedFare(type.getBaseRatePerKm().multiply(BigDecimal.valueOf(km)))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .assignedTruckId(truckId).status(status).build());
    }
}
