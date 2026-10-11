package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.*;
import com.jubayer.cholobodlai.Model.*;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Repository.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final TripRepository trips;
    private final BidRepository bids;
    private final CurrentUserService current;
    public ReviewService(ReviewRepository reviews, TripRepository trips,
                         BidRepository bids, CurrentUserService current) {
        this.reviews = reviews; this.trips = trips; this.bids = bids; this.current = current;
    }
    public void submit(String tripId, int stars, String comment) {
        User actor = current.currentUser();
        Trip trip = trips.findById(tripId).orElseThrow(() ->
                new ResourceNotFoundException("Trip", tripId));
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new BusinessRuleException("Reviews are available after the delivery is completed");
        }
        String cleanComment = MarketplaceRules.review(stars, comment);
        Bid winner = bids.findByTripIdAndStatus(tripId, BidStatus.ACCEPTED)
                .stream().findFirst().orElseThrow(() -> new BusinessRuleException("No accepted offer"));
        String subject;
        if (actor.getRole() == Role.SHIPPER && actor.getId().equals(trip.getShipperId())) {
            subject = winner.getOwnerId();
        } else if (actor.getRole() == Role.OWNER && actor.getId().equals(winner.getOwnerId())) {
            subject = trip.getShipperId();
        } else throw new AccessDeniedException("Only participants may review this trip");
        try {
            reviews.insert(Review.builder().id("review:" + tripId + ":" + actor.getId())
                    .tripId(tripId).authorId(actor.getId()).subjectId(subject)
                    .authorName(actor.getFullName()).stars(stars)
                    .comment(cleanComment)
                    .createdAt(LocalDateTime.now()).build());
        } catch (DuplicateKeyException e) {
            throw new BusinessRuleException("You already reviewed this delivery");
        }
    }
}
