package com.jubayer.cholobodlai.Controller.n;

import com.jubayer.cholobodlai.EXCEPTION.*;
import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.Emums.TruckStatus;
import com.jubayer.cholobodlai.Model.Emums.BidStatus;
import com.jubayer.cholobodlai.Repository.BidRepository;
import com.jubayer.cholobodlai.Model.Review;
import com.jubayer.cholobodlai.Model.Truck.Truck;
import com.jubayer.cholobodlai.Repository.ReviewRepository;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;
import com.jubayer.cholobodlai.Service.CurrentUserService;
import com.jubayer.cholobodlai.Service.TruckDocumentStorage;
import com.jubayer.cholobodlai.Service.Truck.TruckService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.net.URI;

@Controller
@RequestMapping("/trucks")
public class TruckWebController {
    private final TruckService service;
    private final TruckRepository trucks;
    private final ReviewRepository reviews;
    private final TruckDocumentStorage storage;
    private final BidRepository bids;
    private final CurrentUserService current;
    public TruckWebController(TruckService service, TruckRepository trucks, ReviewRepository reviews,
                              TruckDocumentStorage storage, CurrentUserService current, BidRepository bids) {
        this.service = service; this.trucks = trucks; this.reviews = reviews;
        this.storage = storage; this.current = current; this.bids = bids;
    }
    @GetMapping
    public String list(Model model) {
        var owner = current.currentUser();
        var fleet = owner.getRole() == Role.ADMIN ? trucks.findAll() : trucks.findByOwnerId(owner.getId());
        model.addAttribute("trucks", fleet);
        model.addAttribute("ownerView", owner.getRole() == Role.OWNER);
        Map<String, String> ratings = new HashMap<>();
        Map<String, List<Review>> reviewsByTruck = new HashMap<>();
        for (Truck truck : fleet) {
            // Only feedback written about this owner is shown on their vehicle page.
            // Owner-to-shipper ratings use a different subjectId and are not included.
            List<Review> saved = reviews.findBySubjectId(truck.getOwnerId())
                    .stream()
                    .sorted(Comparator.comparing(Review::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            reviewsByTruck.put(truck.getId(), saved);
            if (!saved.isEmpty()) {
                double average = saved.stream().mapToInt(x -> x.getStars()).average().orElse(0);
                ratings.put(truck.getId(), String.format(Locale.US, "%.1f / 5 (%d reviews)", average, saved.size()));
            }
        }
        model.addAttribute("ratings", ratings);
        model.addAttribute("reviewsByTruck", reviewsByTruck);
        return "trucks/list";
    }
    @GetMapping("/new")
    public String newForm() { return "redirect:/owner"; }

    private Truck permitted(String id) {
        var truck = trucks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Truck", id));
        var account = current.currentUser();
        if (account.getRole() != Role.ADMIN && !account.getId().equals(truck.getOwnerId()))
            throw new org.springframework.security.access.AccessDeniedException("Vehicle belongs to another owner");
        return truck;
    }
    @GetMapping("/{id}/photo")
    public ResponseEntity<?> photo(@PathVariable String id) {
        Truck truck = permitted(id);
        if (truck.getPhotoPath() == null) throw new ResourceNotFoundException("Truck photo", id);
        if (storage.isCloudinaryReference(truck.getPhotoPath())) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(storage.publicPhotoUrl(truck.getPhotoPath())))
                    .cacheControl(CacheControl.noStore()).build();
        }
        Resource resource = storage.load(truck.getPhotoPath());
        String lower = truck.getPhotoPath().toLowerCase(Locale.ROOT);
        MediaType content = lower.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(content).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(resource);
    }
    @PostMapping("/{id}/photo")
    public String uploadPhoto(@PathVariable String id, @RequestParam MultipartFile photo) {
        Truck truck = permitted(id);
        String mime = Objects.toString(photo.getContentType(), "");
        String name = Objects.toString(photo.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (photo.isEmpty() || !Set.of("image/jpeg", "image/png").contains(mime)
                || !(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")))
            throw new BusinessRuleException("Choose a JPG or PNG vehicle photo");
        String stored = storage.storePhoto(photo); // validates actual file signature and size
        String old = truck.getPhotoPath();
        truck.setPhotoPath(stored);
        try { trucks.save(truck); } catch (RuntimeException ex) { storage.deleteQuietly(stored); throw ex; }
        storage.deleteQuietly(old);
        return "redirect:/trucks?photoUpdated";
    }
    /**
     * Re-upload a document that existed only on the local computer. An approved owner's
     * replacement must be checked by an admin before the truck accepts more bids.
     */
    @PostMapping("/{id}/document")
    public String uploadDocument(@PathVariable String id, @RequestParam MultipartFile document) {
        Truck truck = permitted(id);
        var actor = current.currentUser();
        if (actor.getRole() != Role.OWNER) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the registered owner can replace their truck document");
        }
        if (truck.getStatus() == TruckStatus.ON_TRIP) {
            throw new BusinessRuleException("Finish the active delivery before changing the registration document");
        }
        String stored = storage.store(document);
        String old = truck.getDocumentPath();
        truck.setDocumentPath(stored);
        truck.setDocumentName("Truck registration document");
        truck.setVerified(false);
        truck.setStatus(TruckStatus.INACTIVE);
        try {
            trucks.save(truck);
            // Prevent offers with a temporarily unverified truck from remaining selectable.
            for (var bid : bids.findByOwnerIdAndStatus(actor.getId(), BidStatus.PENDING)) {
                bid.setStatus(BidStatus.WITHDRAWN);
                bids.save(bid);
            }
        } catch (RuntimeException ex) {
            storage.deleteQuietly(stored);
            throw ex;
        }
        storage.deleteQuietly(old);
        return "redirect:/trucks?documentUpdated";
    }

}
