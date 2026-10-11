package com.jubayer.cholobodlai.Service;
import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Locale;
@Service
public class ProfileService {
    private final CurrentUserService current;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    public ProfileService(CurrentUserService current, UserRepository users, PasswordEncoder encoder) {
        this.current = current; this.users = users; this.encoder = encoder;
    }
    /** Returns the changed account when the new email must be verified; otherwise null. */
    public User update(String fullName, String email, String currentPassword) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 120)
            throw new BusinessRuleException("Enter a valid name of up to 120 characters");
        if (email == null || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 180)
            throw new BusinessRuleException("A valid contact email is required");
        User u = current.currentUser();
        String newEmail = email.trim().toLowerCase(Locale.ROOT);
        boolean changed = !newEmail.equalsIgnoreCase(u.getEmail() == null ? "" : u.getEmail());
        if (changed && !encoder.matches(currentPassword == null ? "" : currentPassword, u.getPassword()))
            throw new BusinessRuleException("Confirm your password to change your email address");
        if (changed && users.findByEmailIgnoreCase(newEmail).stream().anyMatch(other -> !other.getId().equals(u.getId())))
            throw new BusinessRuleException("This email address is already registered");
        u.setFullName(fullName.trim());
        u.setEmail(newEmail);
        if (changed) u.setEmailVerified(Boolean.FALSE);
        users.save(u);
        return changed ? u : null;
    }
    public void changePassword(String oldPassword, String newPassword) {
        User u = current.currentUser();
        if (!encoder.matches(oldPassword == null ? "" : oldPassword, u.getPassword()))
            throw new BusinessRuleException("Current password is incorrect");
        if (newPassword == null || newPassword.length() < 10 || newPassword.length() > 100)
            throw new BusinessRuleException("New password must be between 10 and 100 characters");
        if (encoder.matches(newPassword, u.getPassword()))
            throw new BusinessRuleException("Please choose a different password");
        u.setPassword(encoder.encode(newPassword));
        u.setPasswordChangedAt(LocalDateTime.now());
        users.save(u);
    }
}
