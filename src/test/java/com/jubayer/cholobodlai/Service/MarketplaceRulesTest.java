package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.Model.Emums.Role;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MarketplaceRulesTest {
    @Test void registrationBoundaries() {
        assertDoesNotThrow(() -> MarketplaceRules.accountName("Rahim"));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.accountName("x".repeat(121)));
        assertDoesNotThrow(() -> MarketplaceRules.phone("01712345678"));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.phone("017123456789"));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.phone("02012345678"));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.password("123456"));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.password("a".repeat(101)));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.email("broken@@address"));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.registration("#".repeat(6)));
        assertDoesNotThrow(() -> MarketplaceRules.registration("DHAKA METRO 42-113"));
    }
    @Test void bidAndReviewBounds() {
        assertDoesNotThrow(() -> MarketplaceRules.bid(new BigDecimal("0.01")));
        assertDoesNotThrow(() -> MarketplaceRules.bid(new BigDecimal("50000000.00")));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.bid(new BigDecimal("50000000.01")));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.bid(new BigDecimal("1.234")));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.bid(BigDecimal.ZERO));
        assertEquals("Good trip", MarketplaceRules.review(5, "  Good trip  "));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.review(6, "test"));
        assertThrows(BusinessRuleException.class, () -> MarketplaceRules.review(4, "a".repeat(501)));
    }
    @Test void publicRolesCannotAccessPrivateTransactions() {
        assertTrue(MarketplaceRules.canSeeTrip(Role.SHIPPER, "shipper1", "shipper1", false, false));
        assertFalse(MarketplaceRules.canSeeTrip(Role.SHIPPER, "shipper2", "shipper1", true, false));
        assertTrue(MarketplaceRules.canSeeTrip(Role.OWNER, "owner1", "shipper1", true, false));
        assertFalse(MarketplaceRules.canSeeTrip(Role.OWNER, "owner2", "shipper1", false, false));
        assertTrue(MarketplaceRules.canSeeTrip(Role.OWNER, "owner1", "shipper1", false, true));
        assertTrue(MarketplaceRules.canSeeTrip(Role.ADMIN, "admin1", "shipper1", false, false));
        assertTrue(MarketplaceRules.canBidWithTruck(Role.OWNER, "owner1", "owner1"));
        assertFalse(MarketplaceRules.canBidWithTruck(Role.OWNER, "owner2", "owner1"));
        assertFalse(MarketplaceRules.canBidWithTruck(Role.SHIPPER, "shipper1", "owner1"));
        assertFalse(MarketplaceRules.canAcceptBid(Role.SHIPPER, "shipper2", "shipper1"));
        assertTrue(MarketplaceRules.canAcceptBid(Role.SHIPPER, "shipper1", "shipper1"));
        assertFalse(MarketplaceRules.canSeeMatchedBooking(Role.OWNER, "otherOwner", "shipper1", "winner"));
        assertTrue(MarketplaceRules.canSeeMatchedBooking(Role.ADMIN, "admin", "shipper1", "winner"));
        assertTrue(MarketplaceRules.canSeeMatchedBooking(Role.OWNER, "winner", "shipper1", "winner"));
    }
    @Test void searchBounds() {
        assertEquals("dhaka", InputPolicy.searchTerm(" DHAKA  "));
        assertThrows(BusinessRuleException.class, () -> InputPolicy.searchTerm("x".repeat(81)));
        assertEquals("COMPLETED", InputPolicy.statusTerm("completed", java.util.Set.of("COMPLETED")));
        assertThrows(BusinessRuleException.class, () -> InputPolicy.statusTerm("hacked", java.util.Set.of("COMPLETED")));
        assertThrows(BusinessRuleException.class, () -> InputPolicy.statusTerm("x".repeat(90), java.util.Set.of("COMPLETED")));
    }
}
