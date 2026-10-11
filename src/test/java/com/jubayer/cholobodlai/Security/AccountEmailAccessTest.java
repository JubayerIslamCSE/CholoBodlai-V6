package com.jubayer.cholobodlai.Security;

import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.Emums.ApprovalStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Important: legacy Atlas users stay active, while newly registered users must verify email. */
class AccountEmailAccessTest {
    private User account(Boolean verified, Role role, ApprovalStatus approval, boolean enabled) {
        return User.builder().id("account").phone("01700001234").email("demo@example.com")
                .password("hashed-password").fullName("Demo")
                .emailVerified(verified).enabled(enabled).role(role).approvalStatus(approval).build();
    }
    @Test void existingAccountWithoutVerificationFieldRetainsAccess() {
        User legacy = account(null, Role.SHIPPER, ApprovalStatus.APPROVED, true);
        assertTrue(legacy.effectiveEmailVerified());
        assertTrue(new AppUserDetails(legacy).isEnabled());
    }
    @Test void newUnverifiedShipperCannotLogin() {
        User newUser = account(false, Role.SHIPPER, ApprovalStatus.APPROVED, true);
        assertFalse(new AppUserDetails(newUser).isEnabled());
    }
    @Test void verifiedPendingOwnerStillRequiresAdminApproval() {
        User owner = account(true, Role.OWNER, ApprovalStatus.PENDING, false);
        assertFalse(new AppUserDetails(owner).isEnabled());
        owner.setApprovalStatus(ApprovalStatus.APPROVED); owner.setEnabled(true);
        assertTrue(new AppUserDetails(owner).isEnabled());
    }
}
