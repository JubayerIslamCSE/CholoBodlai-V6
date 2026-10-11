package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.Model.Truck.Truck;
import com.jubayer.cholobodlai.Model.Emums.TripStatus;
import com.jubayer.cholobodlai.Model.Emums.TruckStatus;
import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.Emums.ApprovalStatus;
import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.EXCEPTION.ResourceNotFoundException;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;
import com.jubayer.cholobodlai.Service.AdminService;
import com.jubayer.cholobodlai.Service.BookingInteractionService;
import com.jubayer.cholobodlai.Service.TruckDocumentStorage;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.net.URI;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebController {
    private final AdminService admins;
    private final BookingInteractionService interactions;
    private final TruckRepository trucks;
    private final TripRepository trips;
    private final BidRepository bids;
    private final AuditEventRepository audit;
    private final TruckDocumentStorage documents;
    public AdminWebController(AdminService admins, TruckRepository trucks,
              TripRepository trips, BidRepository bids, AuditEventRepository audit,
              TruckDocumentStorage documents, BookingInteractionService interactions) {
        this.interactions = interactions; this.admins = admins; this.trucks = trucks; this.trips = trips; this.bids = bids;
        this.audit = audit; this.documents = documents;
    }
    @GetMapping
    public String dashboard(Model model) {
        List<Truck> allTrucks = trucks.findAll();
        Map<String,Truck> truckByOwner = allTrucks.stream().filter(t -> t.getOwnerId() != null)
                .collect(Collectors.toMap(Truck::getOwnerId, t -> t, (a,b) -> a));
        model.addAttribute("pendingOwners", admins.pendingOwners());
        model.addAttribute("approvedOwners", admins.approvedOwners());
        model.addAttribute("rejectedOwners", admins.rejectedOwners());
        model.addAttribute("suspended", admins.suspended());
        model.addAttribute("shippers", admins.shippers());
        model.addAttribute("newShippers", admins.newShippers());
        model.addAttribute("adminAccounts", admins.admins());
        model.addAttribute("ownerTrucks", truckByOwner);
        model.addAttribute("trucks", allTrucks);
        model.addAttribute("documentsNeedingReview", allTrucks.stream()
                .filter(truck -> !truck.isVerified() && truck.getDocumentPath() != null)
                .filter(truck -> admins.approvedOwners().stream()
                        .anyMatch(owner -> owner.getId().equals(truck.getOwnerId())))
                .toList());
        model.addAttribute("audit", audit.findTop30ByOrderByHappenedAtDesc());
        model.addAttribute("totalTrips", trips.count());
        model.addAttribute("openTrips", trips.countByStatus(TripStatus.OPEN));
        model.addAttribute("completedTrips", trips.countByStatus(TripStatus.COMPLETED));
        model.addAttribute("totalBids", bids.count());
        model.addAttribute("disputes", interactions.allDisputes());
        return "admin/dashboard";
    }
    @PostMapping("/owners/{id}/approve")
    public String approve(@PathVariable String id) { admins.approveOwner(id); return "redirect:/admin?approved"; }
    @PostMapping("/owners/{id}/reject")
    public String reject(@PathVariable String id) { admins.rejectOwner(id); return "redirect:/admin?rejected"; }
    @PostMapping("/users/{id}/suspend")
    public String suspend(@PathVariable String id) { admins.suspend(id); return "redirect:/admin?suspended"; }
    @PostMapping("/users/{id}/reinstate")
    public String reinstate(@PathVariable String id) { admins.reinstate(id); return "redirect:/admin?reinstated"; }
    @GetMapping("/truck-documents/{truckId}")
    public ResponseEntity<?> document(@PathVariable String truckId) {
        Truck truck = trucks.findById(truckId).orElseThrow(
                () -> new ResourceNotFoundException("Truck", truckId));
        if (truck.getDocumentPath() == null) {
            throw new ResourceNotFoundException("Document", truckId);
        }
        if (documents.isCloudinaryReference(truck.getDocumentPath())) {
            // Class-level ADMIN permission must be checked before minting this 60-second link.
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(documents.privateDocumentUrl(truck.getDocumentPath())))
                    .cacheControl(CacheControl.noStore()).build();
        }
        Resource resource = documents.load(truck.getDocumentPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=truck-document")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }
    @PostMapping("/trucks/{truckId}/verify-document")
    public String verifyDocument(@PathVariable String truckId) {
        Truck truck = trucks.findById(truckId)
                .orElseThrow(() -> new ResourceNotFoundException("Truck", truckId));
        if (truck.getDocumentPath() == null || truck.isVerified()
                || truck.getStatus() == TruckStatus.ON_TRIP) {
            throw new BusinessRuleException("Document is not awaiting verification");
        }
        boolean approvedOwner = admins.approvedOwners().stream()
                .anyMatch(u -> u.getId().equals(truck.getOwnerId()));
        if (!approvedOwner) {
            throw new BusinessRuleException("Approve the truck owner account first");
        }
        truck.setVerified(true);
        truck.setStatus(TruckStatus.AVAILABLE);
        trucks.save(truck);
        return "redirect:/admin?documentApproved";
    }

}
