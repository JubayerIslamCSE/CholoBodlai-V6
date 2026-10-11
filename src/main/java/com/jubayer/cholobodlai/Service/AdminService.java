package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.Component.Notification.NotificationService;
import com.jubayer.cholobodlai.EXCEPTION.*;
import com.jubayer.cholobodlai.Model.*;
import com.jubayer.cholobodlai.Model.Truck.Truck;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {
    private final UserRepository users;
    private final TruckRepository trucks;
    private final BidRepository bids;
    private final NotificationService notifications;
    private final AuditService audit;
    private final CurrentUserService current;

    public AdminService(UserRepository users, TruckRepository trucks, BidRepository bids,
                        NotificationService notifications, AuditService audit, CurrentUserService current) {
        this.users = users; this.trucks = trucks; this.bids = bids;
        this.notifications = notifications; this.audit = audit; this.current = current;
    }
    public List<User> shippers() { return users.findByRole(Role.SHIPPER); }
    public List<User> admins() { return users.findByRole(Role.ADMIN); }
    public List<User> newShippers() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        return shippers().stream().filter(u -> u.getCreatedAt() != null
                && !u.getCreatedAt().isBefore(since)).toList();
    }
    public List<User> pendingOwners() { return ownersByStatus(ApprovalStatus.PENDING); }
    public List<User> approvedOwners() { return ownersByStatus(ApprovalStatus.APPROVED); }
    public List<User> rejectedOwners() { return ownersByStatus(ApprovalStatus.REJECTED); }
    public List<User> suspended() { return users.findAll().stream()
            .filter(u -> u.effectiveApprovalStatus() == ApprovalStatus.SUSPENDED).toList(); }
    private List<User> ownersByStatus(ApprovalStatus status) {
        return users.findByRole(Role.OWNER).stream()
                .filter(u -> u.effectiveApprovalStatus() == status).toList();
    }
    private User find(String id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
    private User owner(String id) {
        User u = find(id);
        if (u.getRole() != Role.OWNER) throw new BusinessRuleException("Only owner accounts can be reviewed");
        return u;
    }
    @Transactional
    public void approveOwner(String id) {
        User u = owner(id);
        if (u.effectiveApprovalStatus() == ApprovalStatus.SUSPENDED)
            throw new BusinessRuleException("Use reinstate for a suspended owner");
        List<Truck> owned = trucks.findByOwnerId(id);
        if (owned.size() != 1)
            throw new BusinessRuleException("Owner must have exactly one registered truck");
        Truck t = owned.get(0);
        if (t.getStatus() == TruckStatus.ON_TRIP)
            throw new BusinessRuleException("This truck already has an active delivery");
        t.setVerified(true); t.setStatus(TruckStatus.AVAILABLE); trucks.save(t);
        u.setEnabled(true); u.setApprovalStatus(ApprovalStatus.APPROVED);
        u.setApprovedAt(LocalDateTime.now()); users.save(u);
        notifications.notify(u, "Your truck and account were approved. You can now sign in");
        audit.record(current.currentUser().getId(), "APPROVE_OWNER", id);
    }
    @Transactional
    public void rejectOwner(String id) {
        User u = owner(id);
        for (Truck t : trucks.findByOwnerId(id)) {
            if (t.getStatus() == TruckStatus.ON_TRIP)
                throw new BusinessRuleException("Cannot reject an owner with an active delivery");
            t.setStatus(TruckStatus.INACTIVE); t.setVerified(false); trucks.save(t);
        }
        u.setEnabled(false); u.setApprovalStatus(ApprovalStatus.REJECTED);
        u.setApprovedAt(null); users.save(u);
        audit.record(current.currentUser().getId(), "REJECT_OWNER", id);
    }
    @Transactional
    public void suspend(String id) {
        User u = find(id);
        if (u.getRole() == Role.ADMIN) throw new BusinessRuleException("Admin accounts cannot be suspended here");
        for (Truck t : trucks.findByOwnerId(id)) {
            if (t.getStatus() == TruckStatus.ON_TRIP)
                throw new BusinessRuleException("Resolve active deliveries before suspending the owner");
            t.setStatus(TruckStatus.INACTIVE); trucks.save(t);
        }
        if (u.getRole() == Role.OWNER) for (Bid b : bids.findByOwnerIdAndStatus(id, BidStatus.PENDING)) {
            b.setStatus(BidStatus.WITHDRAWN); bids.save(b);
        }
        u.setEnabled(false); u.setApprovalStatus(ApprovalStatus.SUSPENDED); users.save(u);
        audit.record(current.currentUser().getId(), "SUSPEND_USER", id);
    }
    @Transactional
    public void reinstate(String id) {
        User u = find(id);
        if (u.effectiveApprovalStatus() != ApprovalStatus.SUSPENDED)
            throw new BusinessRuleException("This account is not suspended");
        if (u.getRole() == Role.OWNER) {
            List<Truck> owned = trucks.findByOwnerId(id);
            if (owned.size() != 1 || !owned.get(0).isVerified())
                throw new BusinessRuleException("Owner truck must be verified before reinstatement");
            Truck t = owned.get(0); t.setStatus(TruckStatus.AVAILABLE); trucks.save(t);
        }
        u.setEnabled(true); u.setApprovalStatus(ApprovalStatus.APPROVED); users.save(u);
        notifications.notify(u, "Your account has been reinstated");
        audit.record(current.currentUser().getId(), "REINSTATE_USER", id);
    }
}
