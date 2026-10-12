
package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Service.AccountService;
import com.jubayer.cholobodlai.Service.AccountRecoveryService;
import com.jubayer.cholobodlai.Service.EmailDeliveryService;
import com.jubayer.cholobodlai.Service.EmailVerificationPolicy;
import com.jubayer.cholobodlai.Service.LookupService;
import com.jubayer.cholobodlai.Service.TruckDocumentStorage;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthWebController {

    private final AccountService accounts;
    private final AccountRecoveryService recovery;
    private final LookupService lookups;
    private final TruckRepository trucks;
    private final TruckDocumentStorage documents;
    private final EmailDeliveryService emailService;
    private final EmailVerificationPolicy emailPolicy;

    public AuthWebController(
            AccountService accounts,
            LookupService lookups,
            TruckRepository trucks,
            TruckDocumentStorage documents,
            AccountRecoveryService recovery,
            EmailDeliveryService emailService,
            EmailVerificationPolicy emailPolicy) {

        this.accounts = accounts;
        this.recovery = recovery;
        this.lookups = lookups;
        this.trucks = trucks;
        this.documents = documents;
        this.emailService = emailService;
        this.emailPolicy = emailPolicy;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {

        model.addAttribute(
                "truckTypes",
                lookups.truckTypes()
        );

        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam String password,
            @RequestParam Role role,
            @RequestParam(required = false)
            String truckRegistrationNo,
            @RequestParam(required = false)
            String truckTypeId,
            @RequestParam(required = false)
            MultipartFile truckDocument,
            @RequestParam(required = false)
            MultipartFile truckPhoto,
            Model model) {

        String stored = null;
        String storedPhoto = null;

        try {

            // Require an email provider only if
            // mandatory email verification is enabled.
            if (emailPolicy.required()
                    && !emailService.deliveryReady()) {

                throw new BusinessRuleException(
                        "Registration is temporarily unavailable: email delivery is not configured"
                );
            }

            // Owner document upload
            if (role == Role.OWNER
                    && truckDocument != null
                    && !truckDocument.isEmpty()) {

                stored = documents.store(truckDocument);
            }

            // Owner vehicle photo upload
            if (role == Role.OWNER
                    && truckPhoto != null
                    && !truckPhoto.isEmpty()) {

                String mime = java.util.Objects.toString(
                        truckPhoto.getContentType(), ""
                );

                String name = java.util.Objects.toString(
                        truckPhoto.getOriginalFilename(), ""
                ).toLowerCase(java.util.Locale.ROOT);

                if (!java.util.Set.of(
                        "image/png",
                        "image/jpeg"
                ).contains(mime)
                        || !(name.endsWith(".png")
                        || name.endsWith(".jpg")
                        || name.endsWith(".jpeg"))) {

                    throw new BusinessRuleException(
                            "Truck photo must be JPG or PNG"
                    );
                }

                storedPhoto = documents.storePhoto(
                        truckPhoto
                );
            }

            User user = accounts.register(
                    fullName,
                    phone,
                    email,
                    password,
                    role,
                    truckRegistrationNo,
                    truckTypeId
            );

            if (role == Role.OWNER
                    && (stored != null || storedPhoto != null)) {

                var owned = trucks.findByOwnerId(user.getId());

                if (owned.size() == 1) {

                    var truck = owned.get(0);

                    if (stored != null) {
                        truck.setDocumentPath(stored);
                        truck.setDocumentName(
                                "Truck registration document"
                        );
                    }

                    truck.setPhotoPath(storedPhoto);
                    trucks.save(truck);
                }
            }

            // Send a verification email only when enabled.
            if (emailPolicy.required()) {

                try {
                    recovery.sendVerification(user);

                } catch (IllegalStateException deliveryFailure) {
                    return "redirect:/resend-verification?mailUnavailable";
                }

                return user.getRole() == Role.OWNER
                        ? "redirect:/login?pendingVerify"
                        : "redirect:/login?verifyEmail";
            }

            // Email verification OFF:
            // shipper may sign in immediately.
            // Owner still needs admin approval.
            return user.getRole() == Role.OWNER
                    ? "redirect:/login?pending"
                    : "redirect:/login?registered";

        } catch (BusinessRuleException e) {

            documents.deleteQuietly(stored);
            documents.deleteQuietly(storedPhoto);

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "truckTypes",
                    lookups.truckTypes()
            );

            return "register";
        }
    }
}