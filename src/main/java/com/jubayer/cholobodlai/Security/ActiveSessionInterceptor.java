package com.jubayer.cholobodlai.Security;

import com.jubayer.cholobodlai.Model.Emums.ApprovalStatus;
import com.jubayer.cholobodlai.Repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Revokes old browser sessions when the account is suspended or password changes. */
@Component
public class ActiveSessionInterceptor implements HandlerInterceptor {
    private final UserRepository users;
    public ActiveSessionInterceptor(UserRepository users) { this.users = users; }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails p)) return true;
        boolean valid = users.findById(p.getId()).map(u ->
                u.isEnabled() && u.effectiveEmailVerified() && u.effectiveApprovalStatus() == ApprovalStatus.APPROVED &&
                        u.getRole() == p.getRole() && u.getPassword().equals(p.getPassword()))
                .orElse(false);
        if (valid) return true;
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        response.sendRedirect(request.getContextPath() + "/login?disabled");
        return false;
    }
}
