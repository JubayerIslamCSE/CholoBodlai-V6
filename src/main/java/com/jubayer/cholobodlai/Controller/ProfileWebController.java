
package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Service.*;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
public class ProfileWebController {

    private final CurrentUserService current;
    private final ProfileService profiles;
    private final AccountRecoveryService recovery;
    private final EmailVerificationPolicy emailPolicy;

    public ProfileWebController(
            CurrentUserService current,
            ProfileService profiles,
            AccountRecoveryService recovery,
            EmailVerificationPolicy emailPolicy) {

        this.current = current;
        this.profiles = profiles;
        this.recovery = recovery;
        this.emailPolicy = emailPolicy;
    }

    @GetMapping
    public String page(Model model) {

        model.addAttribute(
                "account",
                current.currentUser()
        );

        return "profile";
    }

    @PostMapping
    public String update(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false)
            String currentPassword,
            HttpServletRequest request) {

        User changed = profiles.update(
                fullName,
                email,
                currentPassword
        );

        if (changed != null && emailPolicy.required()) {

            recovery.sendVerification(changed);

            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }

            return "redirect:/login?verifyEmail";
        }

        return "redirect:/profile?saved";
    }

    @PostMapping("/password")
    public String password(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpServletRequest request) {

        if (!newPassword.equals(confirmPassword)) {

            throw new BusinessRuleException(
                    "Passwords do not match"
            );
        }

        profiles.changePassword(
                currentPassword,
                newPassword
        );

        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        return "redirect:/login?passwordChanged";
    }
}