package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.Model.Emums.Role;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/** Business rules shared by browser forms and REST endpoints. */
public final class MarketplaceRules {
    private MarketplaceRules() {}

    public static void accountName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 120)
            throw new BusinessRuleException("Full name must be between 1 and 120 characters");
    }
    public static void phone(String phone) {
        if (phone == null || !phone.strip().matches("01[3-9][0-9]{8}"))
            throw new BusinessRuleException("Use an 11-digit Bangladeshi mobile number starting with 01");
    }
    public static void password(String password) {
        if (password == null || password.length() < 10 || password.length() > 100)
            throw new BusinessRuleException("Password must contain 10 to 100 characters");
    }
    public static void email(String email) {
        if (email == null || email.isBlank() || email.strip().length() > 180
                || !email.strip().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new BusinessRuleException("A valid email address (up to 180 characters) is required");
    }
    public static void registration(String registration) {
        if (registration == null || !registration.strip().matches("[\\p{L}\\p{N} -]{5,35}"))
            throw new BusinessRuleException("Truck registration needs 5–35 letters, digits, spaces or hyphens");
    }
    public static void bid(BigDecimal amount) {
        if (amount == null || amount.compareTo(new BigDecimal("0.01")) < 0
                || amount.compareTo(new BigDecimal("50000000.00")) > 0 || amount.scale() > 2)
            throw new BusinessRuleException("Bid must be 0.01–50,000,000 with at most two decimal places");
    }
    public static String review(int stars, String comment) {
        if (stars < 1 || stars > 5)
            throw new BusinessRuleException("Choose between 1 and 5 stars");
        String value = comment == null ? "" : comment.strip();
        if (value.length() > 500)
            throw new BusinessRuleException("Review description must be at most 500 characters");
        return value;
    }
    public static boolean canSeeTrip(Role role, String actorId, String shipperId,
                                     boolean isOpen, boolean acceptedOwner) {
        return role == Role.ADMIN || (role == Role.SHIPPER && Objects.equals(actorId, shipperId))
                || (role == Role.OWNER && (isOpen || acceptedOwner));
    }
    public static boolean canBidWithTruck(Role role, String actorId, String truckOwnerId) {
        return role == Role.ADMIN || (role == Role.OWNER && Objects.equals(actorId, truckOwnerId));
    }
    public static boolean canAcceptBid(Role role, String actorId, String shipperId) {
        return role == Role.ADMIN || (role == Role.SHIPPER && Objects.equals(actorId, shipperId));
    }
    public static boolean canSeeMatchedBooking(Role role, String actorId, String shipperId, String ownerId) {
        return role == Role.ADMIN || Objects.equals(actorId, shipperId) || Objects.equals(actorId, ownerId);
    }
}
