package com.jubayer.cholobodlai.Security;

import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserDetails implements UserDetails {
    private final String id;          // was Long
    private final String phone;
    private final String fullName;
    private final String password;
    private final Role role;
    private final boolean enabled;
    private final java.time.LocalDateTime passwordChangedAt;

    public AppUserDetails(User u) {
        this.id = u.getId();
        this.phone = u.getPhone();
        this.fullName = u.getFullName();
        this.password = u.getPassword();
        this.role = u.getRole();
        this.passwordChangedAt = u.getPasswordChangedAt();
        this.enabled = u.effectiveEmailVerified() && u.isEnabled()
                && u.effectiveApprovalStatus()
                == com.jubayer.cholobodlai.Model.Emums.ApprovalStatus.APPROVED;
    }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return phone; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }

    public String getId() { return id; }           // was Long
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public java.time.LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
}