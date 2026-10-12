
package com.jubayer.cholobodlai.Security;

import com.jubayer.cholobodlai.Model.Emums.ApprovalStatus;
import com.jubayer.cholobodlai.Repository.UserRepository;
import com.jubayer.cholobodlai.Service.EmailVerificationPolicy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActiveSessionInterceptor implements HandlerInterceptor {

    private final UserRepository users;
    private final EmailVerificationPolicy emailPolicy;

    public ActiveSessionInterceptor(
            UserRepository users,
            EmailVerificationPolicy emailPolicy) {

        this.users = users;
        this.emailPolicy = emailPolicy;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        var auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null
                || !(auth.getPrincipal() instanceof AppUserDetails p)) {
            return true;
        }

        boolean valid = users.findById(p.getId())
                .map(user ->
                        user.isEnabled()
                                && emailPolicy.permitted(user)
                                && user.effectiveApprovalStatus()
                                == ApprovalStatus.APPROVED
                                && user.getRole() == p.getRole()
                                && user.getPassword().equals(p.getPassword())
                )
                .orElse(false);

        if (valid) {
            return true;
        }

        SecurityContextHolder.clearContext();

        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        response.sendRedirect(
                request.getContextPath() + "/login?disabled"
        );

        return false;
    }
}