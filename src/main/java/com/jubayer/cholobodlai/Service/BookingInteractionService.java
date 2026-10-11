package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.*;
import com.jubayer.cholobodlai.Model.*;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Component.Notification.NotificationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingInteractionService {
    private final TripRepository trips;
    private final BidRepository bids;
    private final UserRepository users;
    private final BookingMessageRepository messages;
    private final DisputeRepository disputes;
    private final CurrentUserService current;
    private final NotificationService notifications;
    private final AuditService audit;
    public BookingInteractionService(TripRepository trips, BidRepository bids, UserRepository users,
            BookingMessageRepository messages, DisputeRepository disputes,
            CurrentUserService current, NotificationService notifications, AuditService audit) {
        this.trips = trips; this.bids = bids; this.users = users;
        this.messages = messages; this.disputes = disputes;
        this.current = current; this.notifications = notifications; this.audit = audit;
    }
    public record MatchedTrip(Trip trip, Bid accepted, User shipper, User owner) {}
    public MatchedTrip match(String tripId) {
        Trip t = trips.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
        // No access to booking messages, disputes or receipts before assignment.
        if (t.getStatus() == TripStatus.OPEN || t.getStatus() == TripStatus.CANCELLED || t.getStatus() == TripStatus.BIDDING_CLOSED)
            throw new BusinessRuleException("These details become available after a bid is accepted");
        Bid accepted = bids.findByTripIdAndStatus(tripId, BidStatus.ACCEPTED).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("No accepted bid exists for this trip"));
        User actor = current.currentUser();
        if (!MarketplaceRules.canSeeMatchedBooking(
                actor.getRole(), actor.getId(), t.getShipperId(), accepted.getOwnerId()))
            throw new AccessDeniedException("Only the matched participants may see this booking");
        User shipper = users.findById(t.getShipperId()).orElseThrow();
        User owner = users.findById(accepted.getOwnerId()).orElseThrow();
        return new MatchedTrip(t, accepted, shipper, owner);
    }
    public List<BookingMessage> messages(String tripId) {
        match(tripId);
        List<BookingMessage> items = new ArrayList<>(messages.findTop100ByTripIdOrderByCreatedAtDesc(tripId));
        Collections.reverse(items);
        return items;
    }
    @Transactional
    public void sendMessage(String tripId, String content) {
        MatchedTrip match = match(tripId);
        if (content == null || content.isBlank() || content.length() > 1500)
            throw new BusinessRuleException("Message must contain 1 to 1500 characters");
        User actor = current.currentUser();
        messages.insert(BookingMessage.builder().tripId(tripId).senderId(actor.getId())
                .senderName(actor.getFullName()).body(content.trim()).createdAt(LocalDateTime.now()).build());
        if (!actor.getId().equals(match.shipper().getId()))
            notifications.notify(match.shipper(), "New message about your confirmed booking");
        if (!actor.getId().equals(match.owner().getId()))
            notifications.notify(match.owner(), "New message about your confirmed booking");
    }
    public List<Dispute> disputesForTrip(String tripId) {
        match(tripId);
        return disputes.findByTripIdOrderByCreatedAtDesc(tripId);
    }
    @Transactional
    public void openDispute(String tripId, String reason) {
        match(tripId);
        User actor = current.currentUser();
        if (reason == null || reason.isBlank() || reason.length() > 2000)
            throw new BusinessRuleException("Describe the issue in up to 2000 characters");
        if (disputes.existsByTripIdAndOpenedByIdAndStatus(tripId, actor.getId(), Dispute.Status.OPEN))
            throw new BusinessRuleException("Your report for this booking is already open");
        disputes.insert(Dispute.builder().tripId(tripId).openedById(actor.getId())
                .openedByName(actor.getFullName()).description(reason.trim())
                .status(Dispute.Status.OPEN).createdAt(LocalDateTime.now()).build());
        for (User admin : users.findByRole(Role.ADMIN)) notifications.notify(admin, "New booking dispute to review");
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resolveDispute(String id, String response) {
        if (response == null || response.isBlank() || response.length() > 2000)
            throw new BusinessRuleException("Write a resolution of 1–2000 characters");
        Dispute d = disputes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dispute", id));
        if (d.getStatus() != Dispute.Status.OPEN)
            throw new BusinessRuleException("This report has already been resolved");
        d.setStatus(Dispute.Status.RESOLVED);
        d.setAdminResponse(response.trim());
        d.setReviewedById(current.currentUser().getId());
        d.setResolvedAt(LocalDateTime.now());
        disputes.save(d);
        users.findById(d.getOpenedById()).ifPresent(u -> notifications.notify(u, "Your booking report received an admin resolution"));
        audit.record(current.currentUser().getId(), "DISPUTE_RESOLVED", id);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public List<Dispute> allDisputes() { return disputes.findAllByOrderByCreatedAtDesc(); }
}
