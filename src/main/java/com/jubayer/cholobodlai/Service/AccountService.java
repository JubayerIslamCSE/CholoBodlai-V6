
package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.EXCEPTION.ResourceNotFoundException;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Model.Truck.*;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Repository.UserRepository;
import com.jubayer.cholobodlai.Repository.Truck.*;
import com.jubayer.cholobodlai.Component.Notification.NotificationService;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;

@Service
public class AccountService {

    private final UserRepository users;
    private final TruckRepository trucks;
    private final TruckTypeRepository types;
    private final PasswordEncoder encoder;
    private final NotificationService notifications;
    private final EmailDeliveryService emailDelivery;

    public AccountService(
            UserRepository users,
            TruckRepository trucks,
            TruckTypeRepository types,
            PasswordEncoder encoder, NotificationService notifications,
            EmailDeliveryService emailDelivery) {

        this.users = users;
        this.trucks = trucks;
        this.types = types;
        this.encoder = encoder;
        this.notifications = notifications;
        this.emailDelivery = emailDelivery;
    }

    @Transactional
    public User register(
            String fullName,
            String phone,
            String email,
            String password,
            Role role,
            String registrationNo,
            String truckTypeId) {

        if (!emailDelivery.deliveryReady()) {
            throw new BusinessRuleException("Registration is temporarily unavailable until email delivery is configured");
        }
        if (role != Role.SHIPPER && role != Role.OWNER) {
            throw new BusinessRuleException(
                    "Choose Shipper or Truck Owner");
        }

        MarketplaceRules.accountName(fullName);
        MarketplaceRules.phone(phone);
        MarketplaceRules.password(password);
        MarketplaceRules.email(email);
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!users.findByEmailIgnoreCase(cleanEmail).isEmpty()) {
            throw new BusinessRuleException("This email address is already registered");
        }
        String cleanPhone = phone.trim();

        if (users.findByPhone(cleanPhone).isPresent()) {
            throw new BusinessRuleException(
                    "Phone number already registered");
        }

        TruckType type = null;
        String reg = null;

        if (role == Role.OWNER) {

            if (registrationNo == null
                    || registrationNo.isBlank()
                    || truckTypeId == null
                    || truckTypeId.isBlank()) {

                throw new BusinessRuleException(
                        "Truck number and type are required for owners");
            }

            reg = registrationNo.trim().toUpperCase(Locale.ROOT);
            MarketplaceRules.registration(reg);

            if (trucks.existsByRegistrationNo(reg)) {
                throw new BusinessRuleException(
                        "Truck registration number already registered");
            }

            type = types.findById(truckTypeId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "TruckType", truckTypeId));
        }

        boolean shipper = role == Role.SHIPPER;
        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .fullName(fullName.trim())
                .phone(cleanPhone)
                .email(cleanEmail)
                .emailVerified(Boolean.FALSE)
                .password(encoder.encode(password))
                .role(role)
                .enabled(shipper)
                .approvalStatus(
                        shipper
                                ? ApprovalStatus.APPROVED
                                : ApprovalStatus.PENDING)
                .createdAt(now)
                .approvedAt(shipper ? now : null)
                .build();

        try {
            user = users.insert(user);

            if (role == Role.OWNER) {

                Truck truck = Truck.builder()
                        .id("owner-truck:" + user.getId())
                        .registrationNo(reg)
                        .ownerId(user.getId())
                        .ownerName(user.getFullName())
                        .truckTypeId(type.getId())
                        .truckTypeName(type.getName())
                        .categoryName(type.getCategoryName())
                        .status(TruckStatus.INACTIVE)
                        .verified(false)
                        .build();

                trucks.insert(truck);
                for (User admin : users.findByRole(Role.ADMIN)) {
                    notifications.notify(admin, "New owner registration awaiting approval: " + user.getFullName());
                }
            }

        } catch (DuplicateKeyException ex) {

            throw new BusinessRuleException(
                    "Phone or truck number already registered");
        }

        return user;
    }
}
