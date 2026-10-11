package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.ResourceNotFoundException;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Repository.UserRepository;
import com.jubayer.cholobodlai.Security.AppUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;
    public CurrentUserService(UserRepository users) { this.users = users; }

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUserDetails p)) {
            throw new AccessDeniedException("No authenticated user");
        }
        User user = users.findById(p.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", p.getId()));

        if (!user.effectiveEmailVerified() || !user.isEnabled() ||
                user.effectiveApprovalStatus()
                        != com.jubayer.cholobodlai.Model.Emums.ApprovalStatus.APPROVED) {
            throw new AccessDeniedException("Account not active");
        }

        return user;
    }
}